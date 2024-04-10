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

import java.time.Instant

import basespecs.BaseControllerSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector

import scala.concurrent.Future

class GroupAccountControllerSpec extends BaseControllerSpec {

  "Using the controller with the bstDownstream feature switch enabled" when {
    "create" should {
      "create a new individual user in modernised" in {
        val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
        val testIndividualOrg =
          IndividualAccountSubmissionForOrganisation("test-external-id", Some("test-trust-id"), testIndividualDetails)
        val testGroupAccountSubmission = GroupAccountSubmission(
          id = "test-group-id",
          companyName = "test-group-id",
          addressId = 1,
          email = "test@test.com",
          phone = "012312321321",
          isAgent = false,
          individualAccountSubmission = testIndividualOrg
        )

        val groupJson = Json.toJson(testGroupAccountSubmission)

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.createGroupAccount(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(GroupId(1, "test", 23)))

        val res = testController.create()(FakeRequest().withBody(groupJson))

        status(res) shouldBe CREATED
        contentAsJson(res) shouldBe Json.toJson(GroupId(1, "test", 23))
      }
    }

    "get" should {
      "return group account json from modernised if it exists" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = false,
            agentCode = Some(1))

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.getDetailedGroupAccount(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.get(1)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.getDetailedGroupAccount(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val res = testController.get(1)(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "withGroupId" should {
      "return group account json from modernised if it exists using the group ID" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = false,
            agentCode = Some(1))

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.withGroupId("test-group-id")(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val res = testController.withGroupId("test-group-id")(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "withAgentCode" should {
      "return group account json from modernised if it exists using the agent code" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = true,
            agentCode = Some(1))

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.withAgentCode(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.withAgentCode("test-agent-code")(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.withAgentCode(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

        val res = testController.withAgentCode("test-agent-code")(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "update" should {
      "update an existing group in modernised" in {
        val testUpdatedOrgAccount = UpdatedOrganisationAccount(
          governmentGatewayGroupId = "test-group-id",
          addressUnitId = 1,
          representativeFlag = false,
          organisationName = "Test Company",
          organisationEmailAddress = "test@test.com",
          organisationTelephoneNumber = "0123456778",
          effectiveFrom = Instant.now(),
          changedByGGExternalId = "test-external-id"
        )

        val testUpdatedOrgAccountJson = Json.toJson(testUpdatedOrgAccount)

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockCustomerManagementApi.updateGroupAccount(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        when(mockBrAuth.clearCache()(any[HeaderCarrier])).thenReturn(Future.successful(()))

        val res = testController.update(1)(FakeRequest().withBody(testUpdatedOrgAccountJson))

        status(res) shouldBe OK
      }
    }
  }

  "Using the controller with the bstDownstream feature switch disabled" when {
    "create" should {
      "create a new individual user in modernised" in {
        val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
        val testIndividualOrg =
          IndividualAccountSubmissionForOrganisation("test-external-id", Some("test-trust-id"), testIndividualDetails)
        val testGroupAccountSubmission = GroupAccountSubmission(
          id = "test-group-id",
          companyName = "test-group-id",
          addressId = 1,
          email = "test@test.com",
          phone = "012312321321",
          isAgent = false,
          individualAccountSubmission = testIndividualOrg
        )

        val groupJson = Json.toJson(testGroupAccountSubmission)

        when(mockModernisedCustomerManagementApi.createGroupAccount(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(GroupId(1, "test", 23)))

        val res = testController.create()(FakeRequest().withBody(groupJson))

        status(res) shouldBe CREATED
        contentAsJson(res) shouldBe Json.toJson(GroupId(1, "test", 23))
      }
    }

    "get" should {
      "return group account json from modernised if it exists" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = false,
            agentCode = Some(1))

        when(mockModernisedCustomerManagementApi.getDetailedGroupAccount(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.get(1)(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockModernisedCustomerManagementApi.getDetailedGroupAccount(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val res = testController.get(1)(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "withGroupId" should {
      "return group account json from modernised if it exists using the group ID" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = false,
            agentCode = Some(1))

        when(mockModernisedCustomerManagementApi.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.withGroupId("test-group-id")(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockModernisedCustomerManagementApi.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val res = testController.withGroupId("test-group-id")(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "withAgentCode" should {
      "return group account json from modernised if it exists using the agent code" in {
        val testGroupAccount =
          GroupAccount(
            id = 1,
            groupId = "test-group-id",
            companyName = "Test Company",
            addressId = 1,
            email = "test@test.com",
            phone = "01233421342",
            isAgent = true,
            agentCode = Some(1))

        when(mockModernisedCustomerManagementApi.withAgentCode(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some(testGroupAccount)))

        val res = testController.withAgentCode("test-agent-code")(FakeRequest())

        status(res) shouldBe OK
        contentAsJson(res) shouldBe Json.toJson(testGroupAccount)
      }

      "return NotFound if the account does not exist in modernised" in {
        when(mockModernisedCustomerManagementApi.withAgentCode(any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val res = testController.withAgentCode("test-agent-code")(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }

    "update" should {
      "update an existing group in modernised" in {
        val testUpdatedOrgAccount = UpdatedOrganisationAccount(
          governmentGatewayGroupId = "test-group-id",
          addressUnitId = 1,
          representativeFlag = false,
          organisationName = "Test Company",
          organisationEmailAddress = "test@test.com",
          organisationTelephoneNumber = "0123456778",
          effectiveFrom = Instant.now(),
          changedByGGExternalId = "test-external-id"
        )

        val testUpdatedOrgAccountJson = Json.toJson(testUpdatedOrgAccount)

        when(mockModernisedCustomerManagementApi.updateGroupAccount(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))
        when(mockBrAuth.clearCache()(any[HeaderCarrier])).thenReturn(Future.successful(()))

        val res = testController.update(1)(FakeRequest().withBody(testUpdatedOrgAccountJson))

        status(res) shouldBe OK
      }
    }

  }

  lazy val mockBrAuth: BusinessRatesAuthConnector = mock[BusinessRatesAuthConnector]

  lazy val testController = new GroupAccountController(
    Helpers.stubControllerComponents(),
    preAuthenticatedActionBuilders(),
    mock[AuditingService],
    mockModernisedCustomerManagementApi,
    mockCustomerManagementApi,
    mockFeatureSwitch,
    mockBrAuth
  )

}
