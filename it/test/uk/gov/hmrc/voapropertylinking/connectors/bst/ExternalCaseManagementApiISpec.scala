/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.bst

import models.CanChallengeResponse
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.CheckCaseStatus
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myclients.{CheckCaseWithClient, CheckCasesWithClient, Client}
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myorganisation.{CheckCaseWithAgent, CheckCasesWithAgent}
import uk.gov.hmrc.voapropertylinking.stubs.bst.ExternalCaseManagementStub
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes.INTERNAL_SERVER_ERROR

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class ExternalCaseManagementApiISpec extends BaseIntegrationSpec with ExternalCaseManagementStub {

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    lazy val connector: ExternalCaseManagementApi = app.injector.instanceOf[ExternalCaseManagementApi]
  }

  val propertyLinkSubmissionId = "property-link-id"
  val timeString = "2023-03-31T15:46:43.511983"

  "getMyOrganisationCheckCases" should {
    "return the correct model on success" in new TestSetup {
      val responseJson: JsValue =
        Json.parse(s"""
                      |{
                      |  "start" : 1,
                      |  "size" : 15,
                      |  "filterTotal" : 1,
                      |  "total" : 1,
                      |  "checkCases" : [ {
                      |    "checkCaseSubmissionId" : "CHK-12345",
                      |    "checkCaseReference" : "CHK12345",
                      |    "checkCaseStatus" : "ASSIGNED",
                      |    "address" : "1 Test Road, Acneville",
                      |    "uarn" : 1,
                      |    "createdDateTime" : "$timeString",
                      |    "submittedBy" : "test user",
                      |    "originatingAssessmentReference" : 1
                      |  } ]
                      |}
                      |""".stripMargin)
      val expectedModel: CheckCasesWithAgent = CheckCasesWithAgent(
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
            createdDateTime = LocalDateTime.parse(timeString),
            settledDate = None,
            agent = None,
            submittedBy = "test user"
          )
        )
      )
      stubGetMyOrganisationCheckCases(propertyLinkSubmissionId)(OK, responseJson)

      val result: CheckCasesWithAgent = await(connector.getMyOrganisationCheckCases(propertyLinkSubmissionId))

      result shouldBe expectedModel
    }
    "throw an exception" when {
      "incorrect Json is received" in new TestSetup {
        stubGetMyOrganisationCheckCases(propertyLinkSubmissionId)(OK, Json.obj("incorrect" -> "body"))

        val result: Exception = intercept[Exception] {
          await(connector.getMyOrganisationCheckCases(propertyLinkSubmissionId))
        }

        result shouldBe a[JsValidationException]
      }
      "any error status is received" in new TestSetup {
        stubGetMyOrganisationCheckCases(propertyLinkSubmissionId)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getMyOrganisationCheckCases(propertyLinkSubmissionId))
        }
      }
    }
  }

  "getMyClientsCheckCases" should {
    "return the correct model on success" in new TestSetup {
      val responseJson =
        Json.parse(s"""
                      |{
                      |  "start" : 1,
                      |  "size" : 15,
                      |  "filterTotal" : 1,
                      |  "total" : 1,
                      |  "checkCases" : [ {
                      |    "checkCaseSubmissionId" : "CHK-12345",
                      |    "checkCaseReference" : "CHK12345",
                      |    "checkCaseStatus" : "ASSIGNED",
                      |    "address" : "1 Test Road, Acneville",
                      |    "uarn" : 1,
                      |    "originatingAssessmentReference" : 1,
                      |    "createdDateTime" : "$timeString",
                      |    "client" : {
                      |      "organisationId" : 1,
                      |      "organisationName" : "test acne"
                      |    },
                      |    "submittedBy" : "test user"
                      |  } ]
                      |}
                      |""".stripMargin)
      val expectedModel = CheckCasesWithClient(
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
            createdDateTime = LocalDateTime.parse(timeString),
            settledDate = None,
            client = Client(
              organisationId = 1,
              organisationName = "test acne"
            ),
            submittedBy = "test user"
          )
        )
      )
      stubGetMyClientsCheckCases(propertyLinkSubmissionId)(OK, responseJson)

      val result: CheckCasesWithClient = await(connector.getMyClientsCheckCases(propertyLinkSubmissionId))

      result shouldBe expectedModel
    }
    "return an exception" when {
      "incorrect Json is received" in new TestSetup {
        stubGetMyClientsCheckCases(propertyLinkSubmissionId)(OK, Json.obj("incorrect" -> "body"))

        val result: Exception = intercept[Exception] {
          await(connector.getMyClientsCheckCases(propertyLinkSubmissionId))
        }

        result shouldBe a[JsValidationException]
      }
      "any error status is received" in new TestSetup {
        stubGetMyClientsCheckCases(propertyLinkSubmissionId)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getMyClientsCheckCases(propertyLinkSubmissionId))
        }
      }
    }

    "canChallenge" when {
      val testCheckCaseRef: String = "testCheckCaseRef"
      val testValuationId: Long = 123456L
      "called for client" should {
        "return the correct optional model when a success status is returned with valid json" in new TestSetup {

          val responseJson: JsValue =
            Json.parse("""
                         |{
                         | "result": false,
                         | "reasonCode": "any_thing",
                         | "reason": "anything"
                         |}
                         |""".stripMargin)
          val expectedModel: CanChallengeResponse =
            CanChallengeResponse(result = false, reasonCode = Some("any_thing"), reason = Some("anything"))

          stubCanChallengeClient(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(OK, responseJson)

          val result: Option[CanChallengeResponse] =
            await(
              connector.canChallenge(
                propertyLinkSubmissionId = propertyLinkSubmissionId,
                checkCaseRef = testCheckCaseRef,
                valuationId = testValuationId,
                party = "client"
              )
            )

          result shouldBe Some(expectedModel)
        }

        "return None " when {
          "a success status is returned but with invalid json" in new TestSetup {
            val testCheckCaseRef: String = "testCheckCaseRef"
            val testValuationId: Long = 123456L

            stubCanChallengeClient(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(
              OK,
              Json.obj("incorrect" -> "body")
            )

            val result =
              await(
                connector.canChallenge(
                  propertyLinkSubmissionId = propertyLinkSubmissionId,
                  checkCaseRef = testCheckCaseRef,
                  valuationId = testValuationId,
                  party = "client"
                )
              )

            result shouldBe None
          }
          "any error status is received" in new TestSetup {
            val testCheckCaseRef: String = "testCheckCaseRef"
            val testValuationId: Long = 123456L

            stubCanChallengeClient(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(
              BAD_REQUEST,
              Json.obj("doesnt" -> "matter")
            )

            val result =
              await(
                connector.canChallenge(
                  propertyLinkSubmissionId = propertyLinkSubmissionId,
                  checkCaseRef = testCheckCaseRef,
                  valuationId = testValuationId,
                  party = "client"
                )
              )

            result shouldBe None
          }
        }
      }

      "called for an agent" should {
        "return the correct optional model when a success status is returned with valid json" in new TestSetup {

          val responseJson: JsValue =
            Json.parse("""
                         |{
                         | "result": false,
                         | "reasonCode": "any_thing",
                         | "reason": "anything"
                         |}
                         |""".stripMargin)
          val expectedModel: CanChallengeResponse =
            CanChallengeResponse(result = false, reasonCode = Some("any_thing"), reason = Some("anything"))

          stubCanChallengeAgent(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(OK, responseJson)

          val result: Option[CanChallengeResponse] =
            await(
              connector.canChallenge(
                propertyLinkSubmissionId = propertyLinkSubmissionId,
                checkCaseRef = testCheckCaseRef,
                valuationId = testValuationId,
                party = "agent"
              )
            )

          result shouldBe Some(expectedModel)
        }

        "return None " when {
          "a success status is returned but with invalid json" in new TestSetup {
            val testCheckCaseRef: String = "testCheckCaseRef"
            val testValuationId: Long = 123456L

            stubCanChallengeAgent(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(
              OK,
              Json.obj("incorrect" -> "body")
            )

            val result =
              await(
                connector.canChallenge(
                  propertyLinkSubmissionId = propertyLinkSubmissionId,
                  checkCaseRef = testCheckCaseRef,
                  valuationId = testValuationId,
                  party = "agent"
                )
              )

            result shouldBe None
          }
          "any error status is received" in new TestSetup {
            val testCheckCaseRef: String = "testCheckCaseRef"
            val testValuationId: Long = 123456L

            stubCanChallengeAgent(propertyLinkSubmissionId, testCheckCaseRef, testValuationId)(
              BAD_REQUEST,
              Json.obj("doesnt" -> "matter")
            )

            val result =
              await(
                connector.canChallenge(
                  propertyLinkSubmissionId = propertyLinkSubmissionId,
                  checkCaseRef = testCheckCaseRef,
                  valuationId = testValuationId,
                  party = "agent"
                )
              )

            result shouldBe None
          }
        }
      }

      "called for neither client or agent" should {
        "throw an exception" in new TestSetup {
          val result: Exception = intercept[Exception] {
            await(
              connector.canChallenge(
                propertyLinkSubmissionId = propertyLinkSubmissionId,
                checkCaseRef = testCheckCaseRef,
                valuationId = testValuationId,
                party = "INVALID_PARTY"
              )
            )
          }

          result shouldBe an[IllegalArgumentException]
          result.getMessage shouldBe "Unknown party INVALID_PARTY"
        }
      }
    }
  }
}
