/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import akka.util.ByteString
import basespecs.BaseControllerSpec
import models.voa.valuation.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import models.voa.valuation.dvr.{DetailedValuationRequest, StreamedDocument}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.DVRRecordRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{CCACaseManagementApi, ExternalValuationManagementApi}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DVRCaseManagementSpec extends BaseControllerSpec {

  val testDvr = DetailedValuationRequest(
    authorisationId = 123l,
    organisationId = 1l,
    personId = 2l,
    submissionId = "EMAIL123",
    assessmentRef = 3l,
    agents = None,
    billingAuthorityReferenceNumber = "BAREF"
  )

  "request detailed valuation v2 " should {
    "create a record of the DVR in mongo and POST the DVR to modernised" in {
      val dvrJson = Json.toJson(testDvr)
      val res = testController.requestDetailedValuationV2()(FakeRequest().withBody(dvrJson))

      status(res) shouldBe OK

      verify(mockRepo, times(1)).create(matching(testDvr))
      verify(mockCcaCaseManagementConnector, times(1)).requestDetailedValuation(matching(testDvr))(any[HeaderCarrier])
    }
  }

  "dvr exists" should {
    "return true if the DVR already exists in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((true))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(true)

      verify(mockRepo, times(1)).exists(matching(1l), matching(3l))
      reset(mockRepo)
    }

    "return false if the DVR does not exist in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((false))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(false)

      verify(mockRepo, times(1)).exists(matching(1l), matching(3l))
      reset(mockRepo)
    }
  }

  //TODO flaky test due to now and 0's being removed from result. e.g 10/10/2010T10.10.10.81 vs 10/10/2010T10.10.10.810
  "get dvr documents" should {
    "return 200 OK with the dvr document information" in {
      val now = LocalDateTime.now()

      when(mockExternalValuationManagementapi.getDvrDocuments(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(DvrDocumentFiles(
          checkForm = Document(DocumentSummary("1L", "Check Document", now)),
          detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
        ))))

      val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.parse(
        s"""
           |{
           | "checkForm": {
           |   "documentSummary": {
           |     "documentId": "1L",
           |     "documentName": "Check Document",
           |     "createDatetime": "$now"
           |     }
           | },
           | "detailedValuation": {
           |    "documentSummary": {
           |       "documentId": "2L",
           |       "documentName": "Detailed Valuation Document",
           |       "createDatetime": "$now"
           |    }
           | }
           |}
            """.stripMargin)
    }

    "return 404 NOT_FOUND when the dvr documents dont exists" in {
      when(mockExternalValuationManagementapi.getDvrDocuments(any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }

  "get dvr document" should {
    "return 200 Ok with the file chunked." in {
      when(mockExternalValuationManagementapi.getDvrDocument(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(StreamedDocument(None, None, Map(), Source.single(ByteString(12)))))

      val result = testController.getDvrDocument(1L, 3L, "PL-12345", "1L")(FakeRequest())

      status(result) shouldBe OK
    }
  }

  lazy val testController = new DVRCaseManagement(
    preAuthenticatedActionBuilders(),
    mockCcaCaseManagementConnector,
    mockExternalValuationManagementapi,
    mockRepo)

  lazy val mockExternalValuationManagementapi = mock[ExternalValuationManagementApi]

  lazy val mockRepo = {
    val m = mock[DVRRecordRepository]
    when(m.create(any[DetailedValuationRequest])) thenReturn Future.successful(())
    m
  }

  lazy val mockCcaCaseManagementConnector = {
    val m = mock[CCACaseManagementApi]
    when(m.requestDetailedValuation(any[DetailedValuationRequest])(any[HeaderCarrier])) thenReturn Future.successful(())
    m
  }

}

