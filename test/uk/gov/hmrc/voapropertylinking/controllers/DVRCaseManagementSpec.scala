/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import java.net.URI
import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import akka.util.ByteString
import basespecs.BaseControllerSpec
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import models.modernised.externalvaluationmanagement.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSCookie, WSResponse}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{CCACaseManagementApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.repositories.DVRRecordRepository

import scala.concurrent.Future
import scala.xml.Elem

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

      verify(mockRepo).create(matching(testDvr))
      verify(mockCcaCaseManagementConnector).requestDetailedValuation(matching(testDvr))(any[HeaderCarrier])
    }
  }

  "dvr exists" should {
    "return true if the DVR already exists in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((true))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(true)

      verify(mockRepo).exists(matching(1l), matching(3l))
      reset(mockRepo)
    }

    "return false if the DVR does not exist in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((false))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(false)

      verify(mockRepo).exists(matching(1l), matching(3l))
      reset(mockRepo)
    }
  }

  "get dvr documents" should {
    "return 200 OK with the dvr document information" in {
      val now = LocalDateTime.parse("2019-09-11T11:03:25.123")

      when(mockExternalValuationManagementapi.getDvrDocuments(any(), any(), any())(any()))
        .thenReturn(
          Future.successful(
            Some(
              DvrDocumentFiles(
                checkForm = Document(DocumentSummary("1L", "Check Document", now)),
                detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
              ))))

      val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

      status(result) shouldBe OK
      contentAsJson(result) shouldBe Json.parse(s"""
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
      when(mockExternalValuationManagementapi.getDvrDocuments(any(), any(), any())(any()))
        .thenReturn(Future.successful(None))

      val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

      status(result) shouldBe NOT_FOUND
    }
  }

  "get dvr document" should {
    "return 200 Ok with the file chunked." in {
      val mockWsResponse = {
        val m = new WSResponse {
          override def status: Int = ???

          override def statusText: String = ???

          override def headers: Map[String, Seq[String]] = Map()

          override def underlying[T]: T = ???

          override def cookies: Seq[WSCookie] = ???

          override def cookie(name: String): Option[WSCookie] = ???

          override def body: String = ???

          override def bodyAsBytes: ByteString = ???

          override def bodyAsSource: Source[ByteString, _] = Source.empty[ByteString]

          override def allHeaders: Map[String, Seq[String]] = ???

          override def xml: Elem = ???

          override def json: JsValue = ???

          override def uri: URI = ???
        }
        m
      }

      when(mockExternalValuationManagementapi.getDvrDocument(any(), any(), any(), any())(any()))
        .thenReturn(Future.successful(mockWsResponse))

      val result = testController.getDvrDocument(1L, 3L, "PL-12345", "1L")(FakeRequest())

      status(result) shouldBe OK
    }
  }

  lazy val testController = new DVRCaseManagement(
    Helpers.stubControllerComponents(),
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
