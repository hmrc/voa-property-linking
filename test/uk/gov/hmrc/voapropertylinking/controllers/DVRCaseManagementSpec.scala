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

import java.time.LocalDateTime
import basespecs.BaseControllerSpec
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import models.modernised.externalvaluationmanagement.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.repositories.{DVRRecord, DVRRecordRepository}

import scala.concurrent.Future

class DVRCaseManagementSpec extends BaseControllerSpec {

  val testDvr: DetailedValuationRequest = DetailedValuationRequest(
    authorisationId = 123L,
    organisationId = 1L,
    personId = 2L,
    submissionId = "EMAIL123",
    assessmentRef = 3L,
    agents = None,
    billingAuthorityReferenceNumber = "BAREF"
  )

  val testDvrRecordNoSubId: DVRRecord = DVRRecord(
    organisationId = 1L,
    assessmentRef = 3L,
    agents = Some(List(4L)),
    dvrSubmissionId = None,
    createdAt = Some(LocalDateTime.now())
  )

  val testDvrRecordWithSubId: DVRRecord = DVRRecord(
    organisationId = 1L,
    assessmentRef = 3L,
    agents = Some(List(4L)),
    dvrSubmissionId = Some("DVR123-4567"),
    createdAt = Some(LocalDateTime.now())
  )

  "If the bstDownstream feature switch is enabled" when {

    "request detailed valuation v2 " should {
      "create a record of the DVR in mongo and POST the DVR to modernised" in {
        val dvrJson = Json.toJson(testDvr)

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockRepo.create(testDvr)).thenReturn(Future.successful(()))
        when(mockCCACaseManagementApi.requestDetailedValuation(any[DetailedValuationRequest]())(any()))
          .thenReturn(Future.successful(()))

        val res = testController.requestDetailedValuationV2()(FakeRequest().withBody(dvrJson))

        status(res) shouldBe OK

        verify(mockRepo).create(matching(testDvr))
        verify(mockCCACaseManagementApi).requestDetailedValuation(matching(testDvr))(any[HeaderCarrier])
      }
      "return a 400 (Bad Request) if the body supplied is invalid" in {
        val res =
          testController.requestDetailedValuationV2()(FakeRequest().withBody(Json.obj("invalid" -> "dvrRequest")))

        status(res) shouldBe BAD_REQUEST
      }
    }

    "getDvrRecord" should {
      "return OK with a record with DVR id if the DVR exists in mongo with an id" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(Some(testDvrRecordWithSubId))
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testDvrRecordWithSubId)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }

      "return OK with a record with no DVR id if the DVR already exists in mongo with no id" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(Some(testDvrRecordNoSubId))
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testDvrRecordNoSubId)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }

      "return NOT_FOUND with None if the DVR does not exist in mongo" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(None)
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe NOT_FOUND
        contentAsJson(res) shouldBe Json.toJson(None)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }
    }

    "get dvr documents" should {
      "return 200 OK with the dvr document information" in {
        val now = LocalDateTime.parse("2019-09-11T11:03:25.123")

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockValuationManagementApi.getDvrDocuments(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              Some(
                DvrDocumentFiles(
                  checkForm = Document(DocumentSummary("1L", "Check Document", now)),
                  detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", now))
                ))))

        val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe
          Json.parse(s"""
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
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockValuationManagementApi.getDvrDocuments(any(), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }

    "get dvr document" should {
      "return 200 Ok with the file chunked" in {
        val mockWsResponse: WSResponse = mock[WSResponse]

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockValuationManagementApi.getDvrDocument(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(mockWsResponse))
        when(mockWsResponse.headers).thenReturn(Map("header" -> Seq("value")))

        val result = testController.getDvrDocument(1L, 3L, "PL-12345", "1L")(FakeRequest())

        status(result) shouldBe OK
      }
    }
  }

  "If the bstDownstream feature switch is disabled" when {
    "request detailed valuation v2 " should {
      "create a record of the DVR in mongo and POST the DVR to modernised" in {
        val dvrJson = Json.toJson(testDvr)

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(false)
        when(mockRepo.create(testDvr)).thenReturn(Future.successful(()))
        when(mockModernisedCCACaseManagementApi.requestDetailedValuation(any[DetailedValuationRequest]())(any()))
          .thenReturn(Future.successful(()))

        val res = testController.requestDetailedValuationV2()(FakeRequest().withBody(dvrJson))

        status(res) shouldBe OK

        verify(mockRepo).create(matching(testDvr))
        verify(mockModernisedCCACaseManagementApi).requestDetailedValuation(matching(testDvr))(any[HeaderCarrier])
      }
    }

    "dvr exists" should {
      "return OK with a record with DVR id if the DVR exists in mongo with an id" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(Some(testDvrRecordWithSubId))
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testDvrRecordWithSubId)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }

      "return OK with a record with no DVR id if the DVR already exists in mongo with no id" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(Some(testDvrRecordNoSubId))
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testDvrRecordNoSubId)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }

      "return NOT_FOUND with None if the DVR does not exist in mongo" in {
        when(mockRepo.find(anyLong(), anyLong())) thenReturn Future.successful(None)
        val res = testController.getDvrRecord(1L, 3L)(FakeRequest())

        status(res) shouldBe NOT_FOUND
        contentAsJson(res) shouldBe Json.toJson(None)

        verify(mockRepo).find(matching(1L), matching(3L))
        reset(mockRepo)
      }
    }

    "get dvr documents" should {
      "return 200 OK with the dvr document information" in {
        val now = LocalDateTime.parse("2019-09-11T11:03:25.123")

        when(mockModernisedExternalValuationManagementApi.getDvrDocuments(any(), any(), any())(any()))
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
        when(mockModernisedExternalValuationManagementApi.getDvrDocuments(any(), any(), any())(any()))
          .thenReturn(Future.successful(None))

        val result = testController.getDvrDocuments(1L, 3L, "PL-12345")(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }

    "get dvr document" should {
      "return 200 Ok with the file chunked" in {
        val mockWsResponse: WSResponse = mock[WSResponse]

        when(mockModernisedExternalValuationManagementApi.getDvrDocument(any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(mockWsResponse))
        when(mockWsResponse.headers).thenReturn(Map("header" -> Seq("value")))

        val result = testController.getDvrDocument(1L, 3L, "PL-12345", "1L")(FakeRequest())

        status(result) shouldBe OK
      }
    }
  }

  lazy val testController = new DVRCaseManagement(
    controllerComponents = Helpers.stubControllerComponents(),
    authenticated = preAuthenticatedActionBuilders(),
    modernisedDvrCaseManagement = mockModernisedCCACaseManagementApi,
    modernisedValuationManagementApi = mockModernisedExternalValuationManagementApi,
    dvrCaseManagement = mockCCACaseManagementApi,
    valuationManagementApi = mockValuationManagementApi,
    featureSwitch = mockFeatureSwitch,
    dvrRecordRepository = mockRepo
  )

  lazy val mockRepo = {
    val m = mock[DVRRecordRepository]
    when(m.create(any[DetailedValuationRequest])) thenReturn Future.successful(())
    m
  }
}
