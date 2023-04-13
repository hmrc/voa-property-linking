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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.JsSuccess
import play.api.mvc.Result
import play.api.test.Helpers
import uk.gov.hmrc.test.AllMocks
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.CheckCaseStatus
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myclients.{CheckCaseWithClient, CheckCasesWithClient, Client}
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myorganisation.{CheckCaseWithAgent, CheckCasesWithAgent}

import scala.concurrent.Future

class CheckCaseControllerSpec extends BaseControllerSpec with AllMocks {

  trait Setup {
    val controller = new CheckCaseController(
      Helpers.stubControllerComponents(),
      preAuthenticatedActionBuilders(),
      mockModernisedExternalCaseManagementApi,
      mockCaseManagementApi,
      mockFeatureSwitch)
  }

  "Using the controller with the bstDownstream feature switch disabled" when {
    "retrieving check cases" when {
      "the client has check cases" should {
        "return 200 Ok with correct response" in new Setup {

          val checkCasesWithClient: CheckCasesWithClient =
            CheckCasesWithClient(
              start = 1,
              size = 15,
              filterTotal = 1,
              total = 1,
              checkCases = List(
                CheckCaseWithClient(
                  checkCaseSubmissionId = "CHK-12345",
                  checkCaseReference = "CHK12345",
                  checkCaseStatus = CheckCaseStatus.ASSIGNED,
                  address = "1 Test Road, Acneville",
                  uarn = 1L,
                  originatingAssessmentReference = 1L,
                  createdDateTime = LocalDateTime.now,
                  settledDate = None,
                  client = Client(1L, "test acne"),
                  submittedBy = "test user"
                ))
            )

          when(mockModernisedExternalCaseManagementApi.getMyClientsCheckCases(any())(any()))
            .thenReturn(Future.successful(checkCasesWithClient))

          val result: Future[Result] = controller.getCheckCases("PL1", "agent")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithClient]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe checkCasesWithClient
          }

          verify(mockModernisedExternalCaseManagementApi, times(1)).getMyClientsCheckCases(any())(any())
          verify(mockModernisedExternalCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }

      "the owner has check cases" should {
        "return 200 Ok with correct response" in new Setup {
          val checkCasesWithAgent: CheckCasesWithAgent =
            CheckCasesWithAgent(
              start = 1,
              size = 15,
              filterTotal = 1,
              total = 1,
              checkCases = List(
                CheckCaseWithAgent(
                  checkCaseSubmissionId = "CHK-12345",
                  checkCaseReference = "CHK12345",
                  checkCaseStatus = CheckCaseStatus.ASSIGNED,
                  address = "1 Test Road, Acneville",
                  uarn = 1L,
                  originatingAssessmentReference = 1L,
                  createdDateTime = LocalDateTime.now,
                  settledDate = None,
                  agent = None,
                  submittedBy = "test user"
                ))
            )

          when(mockModernisedExternalCaseManagementApi.getMyOrganisationCheckCases(any())(any()))
            .thenReturn(Future.successful(checkCasesWithAgent))

          val result: Future[Result] = controller.getCheckCases("PL1", "client")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithAgent]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe checkCasesWithAgent
          }

          verify(mockModernisedExternalCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockModernisedExternalCaseManagementApi, times(1)).getMyOrganisationCheckCases(any())(any())
        }
      }

      "their is an error calling modernised the controller" should {
        "return 200 OK with a defaulted CheckCaseWithAgent" in new Setup {

          when(mockModernisedExternalCaseManagementApi.getMyOrganisationCheckCases(any())(any()))
            .thenReturn(Future.failed(new Exception))

          val result: Future[Result] = controller.getCheckCases("PL1", "client")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithAgent]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe CheckCasesWithAgent(1, 100, 0, 0, Nil)
          }

          verify(mockModernisedExternalCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockModernisedExternalCaseManagementApi, times(1)).getMyOrganisationCheckCases(any())(any())
        }

        "return 200 OK with a defaulted CheckCaseWithClient" in new Setup {

          when(mockModernisedExternalCaseManagementApi.getMyClientsCheckCases(any())(any()))
            .thenReturn(Future.failed(new Exception))

          val result: Future[Result] = controller.getCheckCases("PL1", "agent")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithClient]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe CheckCasesWithClient(1, 100, 0, 0, Nil)
          }

          verify(mockModernisedExternalCaseManagementApi, times(1)).getMyClientsCheckCases(any())(any())
          verify(mockModernisedExternalCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }

      "the party (projection) provided is invalid" should {
        "return NOT_IMPLEMENTED with error message" in new Setup {
          val result: Future[Result] = controller.getCheckCases("PL1", "INVALID")(request)

          status(result) shouldBe NOT_IMPLEMENTED
          contentAsString(result) shouldBe "invalid party (projection) supplied: INVALID"

          verify(mockModernisedExternalCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockModernisedExternalCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }
    }
  }

  "Using the controller with the bstDownstream feature switch enabled" when {
    "retrieving check cases" when {
      "the client has check cases" should {
        "return 200 Ok with correct response" in new Setup {

          val checkCasesWithClient: CheckCasesWithClient =
            CheckCasesWithClient(
              start = 1,
              size = 15,
              filterTotal = 1,
              total = 1,
              checkCases = List(
                CheckCaseWithClient(
                  checkCaseSubmissionId = "CHK-12345",
                  checkCaseReference = "CHK12345",
                  checkCaseStatus = CheckCaseStatus.ASSIGNED,
                  address = "1 Test Road, Acneville",
                  uarn = 1L,
                  originatingAssessmentReference = 1L,
                  createdDateTime = LocalDateTime.now,
                  settledDate = None,
                  client = Client(1L, "test acne"),
                  submittedBy = "test user"
                ))
            )

          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockCaseManagementApi.getMyClientsCheckCases(any())(any()))
            .thenReturn(Future.successful(checkCasesWithClient))

          val result: Future[Result] = controller.getCheckCases("PL1", "agent")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithClient]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe checkCasesWithClient
          }

          verify(mockCaseManagementApi, times(1)).getMyClientsCheckCases(any())(any())
          verify(mockCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }

      "the owner has check cases" should {
        "return 200 Ok with correct response" in new Setup {
          val checkCasesWithAgent: CheckCasesWithAgent =
            CheckCasesWithAgent(
              start = 1,
              size = 15,
              filterTotal = 1,
              total = 1,
              checkCases = List(
                CheckCaseWithAgent(
                  checkCaseSubmissionId = "CHK-12345",
                  checkCaseReference = "CHK12345",
                  checkCaseStatus = CheckCaseStatus.ASSIGNED,
                  address = "1 Test Road, Acneville",
                  uarn = 1L,
                  originatingAssessmentReference = 1L,
                  createdDateTime = LocalDateTime.now,
                  settledDate = None,
                  agent = None,
                  submittedBy = "test user"
                ))
            )

          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockCaseManagementApi.getMyOrganisationCheckCases(any())(any()))
            .thenReturn(Future.successful(checkCasesWithAgent))

          val result: Future[Result] = controller.getCheckCases("PL1", "client")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithAgent]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe checkCasesWithAgent
          }

          verify(mockCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockCaseManagementApi, times(1)).getMyOrganisationCheckCases(any())(any())
        }
      }

      "their is an error calling modernised the controller" should {
        "return 200 OK with a defaulted CheckCaseWithAgent" in new Setup {

          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockCaseManagementApi.getMyOrganisationCheckCases(any())(any()))
            .thenReturn(Future.failed(new Exception))

          val result: Future[Result] = controller.getCheckCases("PL1", "client")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithAgent]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe CheckCasesWithAgent(1, 100, 0, 0, Nil)
          }

          verify(mockCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockCaseManagementApi, times(1)).getMyOrganisationCheckCases(any())(any())
        }

        "return 200 OK with a defaulted CheckCaseWithClient" in new Setup {

          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockCaseManagementApi.getMyClientsCheckCases(any())(any()))
            .thenReturn(Future.failed(new Exception))

          val result: Future[Result] = controller.getCheckCases("PL1", "agent")(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[CheckCasesWithClient]) {
            case JsSuccess(checkcases, _) =>
              checkcases shouldBe CheckCasesWithClient(1, 100, 0, 0, Nil)
          }

          verify(mockCaseManagementApi, times(1)).getMyClientsCheckCases(any())(any())
          verify(mockCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }

      "the party (projection) provided is invalid" should {
        "return NOT_IMPLEMENTED with error message" in new Setup {
          val result: Future[Result] = controller.getCheckCases("PL1", "INVALID")(request)

          status(result) shouldBe NOT_IMPLEMENTED
          contentAsString(result) shouldBe "invalid party (projection) supplied: INVALID"

          verify(mockCaseManagementApi, never()).getMyClientsCheckCases(any())(any())
          verify(mockCaseManagementApi, never()).getMyOrganisationCheckCases(any())(any())
        }
      }
    }
  }
}
