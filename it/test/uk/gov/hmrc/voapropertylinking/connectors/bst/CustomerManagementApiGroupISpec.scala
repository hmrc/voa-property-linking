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

import models.{GroupAccount, GroupAccountSubmission, GroupId, UpdatedOrganisationAccount}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, JsValidationException, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.stubs.bst.CustomerManagementStub
import utils.FakeObjects

import java.time.Instant
import scala.concurrent.ExecutionContext

class CustomerManagementApiGroupISpec extends BaseIntegrationSpec with CustomerManagementStub with FakeObjects {

  trait TestSetup {
    lazy val connector: CustomerManagementApi = app.injector.instanceOf[CustomerManagementApi]
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
  }

  "createGroupAccount" should {
    val timeString = "2023-03-29T16:31:17.050540Z"
    val requestJson: JsValue =
      Json.parse(s"""
                    |{
                    |  "governmentGatewayGroupId" : "acc123",
                    |  "organisationName" : "Real news Inc",
                    |  "addressUnitId" : 9876543,
                    |  "organisationEmailAddress" : "thewhitehouse@potus.com",
                    |  "organisationTelephoneNumber" : "01987654",
                    |  "representativeFlag" : false,
                    |  "effectiveFrom" : "$timeString",
                    |  "personData" : {
                    |    "identifyVerificationId" : "trust234",
                    |    "firstName" : "Donald",
                    |    "lastName" : "Trump",
                    |    "addressUnitId" : 24680,
                    |    "telephoneNumber" : "123456789",
                    |    "mobileNumber" : "987654321",
                    |    "emailAddress" : "therealdonald@potus.com",
                    |    "governmentGatewayExternalId" : "Ext123",
                    |    "effectiveFrom" : "$timeString"
                    |  }
                    |}
                    |""".stripMargin.trim)
    val account: GroupAccountSubmission = groupAccountSubmission

    "return a valid GroupID" in new TestSetup {
      val expectedResponse: GroupId = GroupId(id = 654321L, message = "valid group id", responseTime = 45678)
      val responseJson: JsObject = Json.obj(
        "id"           -> 654321L,
        "message"      -> "valid group id",
        "responseTime" -> 45678
      )

      stubCreateGroupAccount(requestJson)(OK, responseJson)

      val result: GroupId = await(connector.createGroupAccount(account, Instant.parse(timeString)))

      result shouldBe expectedResponse
    }
    "return an exception" when {
      "it receives a downstream 4xx response" in new TestSetup {
        stubCreateGroupAccount(requestJson)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.createGroupAccount(account, Instant.parse(timeString)))
        }

        result shouldBe a[VoaClientException]
      }
      "it receives a downstream 5xx response" in new TestSetup {
        stubCreateGroupAccount(requestJson)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.createGroupAccount(account, Instant.parse(timeString)))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

  "updateGroupAccount" should {
    val timeString = "2023-03-29T16:31:17.050540Z"
    val requestJson: JsValue =
      Json.parse(s"""
                    |{
                    |  "governmentGatewayGroupId" : "acc123",
                    |  "addressUnitId" : 9876543,
                    |  "representativeFlag" : false,
                    |  "organisationName" : "Test Inc",
                    |  "organisationEmailAddress" : "test@test.com",
                    |  "organisationTelephoneNumber" : "01987654",
                    |  "effectiveFrom" : "$timeString",
                    |  "changedByGGExternalId" : "test-external-id"
                    |}
                    |""".stripMargin.trim)

    val requestModel = UpdatedOrganisationAccount(
      governmentGatewayGroupId = "acc123",
      addressUnitId = 9876543,
      representativeFlag = false,
      organisationName = "Test Inc",
      organisationEmailAddress = "test@test.com",
      organisationTelephoneNumber = "01987654",
      effectiveFrom = Instant.parse(timeString),
      changedByGGExternalId = "test-external-id"
    )
    "return nothing on success" in new TestSetup {
      val anyOrgId = 123456L
      val expectedHttpResponse: HttpResponse = HttpResponse(OK, Json.obj(), Map.empty)
      stubUpdateGroupAccount(anyOrgId, requestJson)(expectedHttpResponse)

      val result: Unit = await(connector.updateGroupAccount(anyOrgId, requestModel))

      result shouldBe ()
    }
    "return an exception" when {
      "it receives a downstream 4xx response" in new TestSetup {
        val anyOrgId = 123456L
        val expectedHttpResponse: HttpResponse = HttpResponse(BAD_REQUEST, Json.obj(), Map.empty)
        stubUpdateGroupAccount(anyOrgId, requestJson)(expectedHttpResponse)

        val result: Exception = intercept[Exception] {
          await(connector.updateGroupAccount(anyOrgId, requestModel))
        }

        result shouldBe a[VoaClientException]
      }
      "it receives a downstream 5xx response" in new TestSetup {
        val anyOrgId = 123456L
        val expectedHttpResponse: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.obj(), Map.empty)
        stubUpdateGroupAccount(anyOrgId, requestJson)(expectedHttpResponse)

        val result: Exception = intercept[Exception] {
          await(connector.updateGroupAccount(anyOrgId, requestModel))
        }

        result shouldBe a[UpstreamErrorResponse]
      }
    }
  }

  "getDetailedGroupAccount" should {
    val orgId: Long = 123456L
    "return a GroupAccount when a valid JSON body is returned" should {
      "it receives a successful response" in new TestSetup {
        val responseJson: JsValue = Json.parse(s"""
                                                  |{
                                                  | "id": $orgId,
                                                  | "governmentGatewayGroupId":"gggId",
                                                  | "representativeCode": 987654,
                                                  | "organisationLatestDetail": {
                                                  |   "addressUnitId": 345,
                                                  |   "representativeFlag": true,
                                                  |   "organisationName": "Fake News Inc",
                                                  |   "organisationEmailAddress": "test@test.com",
                                                  |   "organisationTelephoneNumber": "9876541"
                                                  | },
                                                  | "persons": []
                                                  |}
                                                  |""".stripMargin)
        val expectedGroupAccount: GroupAccount = GroupAccount(
          id = orgId,
          groupId = "gggId",
          companyName = "Fake News Inc",
          addressId = 345,
          email = "test@test.com",
          phone = "9876541",
          isAgent = true,
          agentCode = Some(987654))

        stubGetDetailedGroupAccount(orgId)(OK, responseJson)

        val result: Option[GroupAccount] = await(connector.getDetailedGroupAccount(orgId))

        result shouldBe Some(expectedGroupAccount)
      }
    }
    "throw an exception" when {
      "it receives a 2xx with an invalid body response" in new TestSetup {
        stubGetDetailedGroupAccount(orgId)(OK, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedGroupAccount(orgId))
        }

        result shouldBe a[JsValidationException]
      }
      "it receives a 4xx response" in new TestSetup {
        stubGetDetailedGroupAccount(orgId)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedGroupAccount(orgId))
        }

        result shouldBe a[VoaClientException]
      }
      "it receives a 5xx response" in new TestSetup {
        stubGetDetailedGroupAccount(orgId)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedGroupAccount(orgId))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

  "findDetailedGroupAccountByGGID" should {
    val ggId = "gggId"
    "return a GroupAccount when a valid JSON body is returned" should {
      "it receives a successful response" in new TestSetup {
        val responseJson: JsValue = Json.parse(s"""
                                                  |{
                                                  | "id": 123456,
                                                  | "governmentGatewayGroupId":"gggId",
                                                  | "representativeCode": 987654,
                                                  | "organisationLatestDetail": {
                                                  |   "addressUnitId": 345,
                                                  |   "representativeFlag": true,
                                                  |   "organisationName": "Fake News Inc",
                                                  |   "organisationEmailAddress": "test@test.com",
                                                  |   "organisationTelephoneNumber": "9876541"
                                                  | },
                                                  | "persons": []
                                                  |}
                                                  |""".stripMargin)
        val expectedGroupAccount: GroupAccount = GroupAccount(
          id = 123456L,
          groupId = ggId,
          companyName = "Fake News Inc",
          addressId = 345,
          email = "test@test.com",
          phone = "9876541",
          isAgent = true,
          agentCode = Some(987654))

        stubFindDetailedGroupAccountByGGID(ggId)(OK, responseJson)

        val result: Option[GroupAccount] = await(connector.findDetailedGroupAccountByGGID(ggId))

        result shouldBe Some(expectedGroupAccount)
      }
    }
    "throw an exception" when {
      "it receives a 2xx with an invalid body response" in new TestSetup {
        stubFindDetailedGroupAccountByGGID(ggId)(OK, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.findDetailedGroupAccountByGGID(ggId))
        }

        result shouldBe a[JsValidationException]
      }
      "it receives a 4xx response" in new TestSetup {
        stubFindDetailedGroupAccountByGGID(ggId)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.findDetailedGroupAccountByGGID(ggId))
        }

        result shouldBe a[VoaClientException]
      }
      "it receives a 5xx response" in new TestSetup {
        stubFindDetailedGroupAccountByGGID(ggId)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.findDetailedGroupAccountByGGID(ggId))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

  "withAgentCode" should {
    val agentCode = "987654"
    "return a GroupAccount when a valid JSON body is returned" should {
      "it receives a successful response" in new TestSetup {
        val responseJson: JsValue =
          Json.parse(s"""
                        |{
                        | "id": 123456,
                        | "governmentGatewayGroupId":"gggId",
                        | "representativeCode": 987654,
                        | "organisationLatestDetail": {
                        |   "addressUnitId": 345,
                        |   "representativeFlag": true,
                        |   "organisationName": "Fake News Inc",
                        |   "organisationEmailAddress": "test@test.com",
                        |   "organisationTelephoneNumber": "9876541"
                        | },
                        | "persons": []
                        |}
                        |""".stripMargin)
        val expectedGroupAccount: GroupAccount = GroupAccount(
          id = 123456L,
          groupId = "gggId",
          companyName = "Fake News Inc",
          addressId = 345,
          email = "test@test.com",
          phone = "9876541",
          isAgent = true,
          agentCode = Some(987654L))

        stubWithAgentCode(agentCode)(OK, responseJson)

        val result: Option[GroupAccount] = await(connector.withAgentCode(agentCode))

        result shouldBe Some(expectedGroupAccount)
      }
    }
    "throw an exception" when {
      "it receives a 2xx with an invalid body response" in new TestSetup {
        stubWithAgentCode(agentCode)(OK, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.withAgentCode(agentCode))
        }

        result shouldBe a[JsValidationException]
      }
      "it receives a 4xx response" in new TestSetup {
        stubWithAgentCode(agentCode)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.withAgentCode(agentCode))
        }

        result shouldBe a[VoaClientException]
      }
      "it receives a 5xx response" in new TestSetup {
        stubWithAgentCode(agentCode)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.withAgentCode(agentCode))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

}
