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

import java.time.Instant

import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import basespecs.BaseControllerSpec
import models._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised.CustomerManagementApi

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class GroupAccountControllerSpec extends BaseControllerSpec {

  "create" should {
    "create a new individual user in modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualOrg = IndividualAccountSubmissionForOrganisation("test-external-id", "test-trust-id", testIndividualDetails)
      val testGroupAccountSubmission = GroupAccountSubmission("test-group-id", "test-group-id", 1, "test@test.com", "012312321321", false, testIndividualOrg)

      val groupJson = Json.toJson(testGroupAccountSubmission)

      when(mockGroupAccountConnector.createGroupAccount(any())(any[HeaderCarrier])).thenReturn(Future.successful(GroupId(1, "test", 23)))

      val res = testController.create()(FakeRequest().withBody(groupJson))
      await(res)

      status(res) mustBe CREATED
      contentAsJson(res) mustBe Json.toJson(GroupId(1, "test", 23))
    }
  }

  "get" should {
    "return group account json from modernised if it exists" in {
      val testGroupAccount = GroupAccount(1, "test-group-id", "Test Company", 1, "test@test.com", "01233421342", false, 1)

      val groupJson = Json.toJson(testGroupAccount)

      when(mockGroupAccountConnector.getDetailedGroupAccount(any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testGroupAccount)))

      val res = testController.get(1)(FakeRequest())
      await(res)

      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(testGroupAccount)
    }

    "return NotFound if the account does not exist in modernised" in {
      when(mockGroupAccountConnector.getDetailedGroupAccount(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val res = testController.get(1)(FakeRequest())
      await(res)

      status(res) mustBe NOT_FOUND
    }
  }

  "withGroupId" should {
    "return group account json from modernised if it exists using the group ID" in {
      val testGroupAccount = GroupAccount(1, "test-group-id", "Test Company", 1, "test@test.com", "01233421342", false, 1)

      val groupJson = Json.toJson(testGroupAccount)

      when(mockGroupAccountConnector.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testGroupAccount)))

      val res = testController.withGroupId("test-group-id")(FakeRequest())
      await(res)

      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(testGroupAccount)
    }

    "return NotFound if the account does not exist in modernised" in {
      when(mockGroupAccountConnector.findDetailedGroupAccountByGGID(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val res = testController.withGroupId("test-group-id")(FakeRequest())
      await(res)

      status(res) mustBe NOT_FOUND
    }
  }

  "withAgentCode" should {
    "return group account json from modernised if it exists using the agent code" in {
      val testGroupAccount = GroupAccount(1, "test-group-id", "Test Company", 1, "test@test.com", "01233421342", true, 1)

      val groupJson = Json.toJson(testGroupAccount)

      when(mockGroupAccountConnector.withAgentCode(any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testGroupAccount)))

      val res = testController.withAgentCode("test-agent-code")(FakeRequest())
      await(res)

      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(testGroupAccount)
    }

    "return NotFound if the account does not exist in modernised" in {
      when(mockGroupAccountConnector.withAgentCode(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val res = testController.withAgentCode("test-agent-code")(FakeRequest())
      await(res)

      status(res) mustBe NOT_FOUND
    }
  }

  "update" should {
    "update an existing group in modernised" in {
      val testUpdatedOrgAccount = UpdatedOrganisationAccount("test-group-id", 1, false, "Test Company", "test@test.com", "0123456778", Instant.now(), "test-external-id")

      val testUpdatedOrgAccountJson = Json.toJson(testUpdatedOrgAccount)

      when(mockGroupAccountConnector.updateGroupAccount(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(()))
      when(mockBrAuth.clearCache()(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val res = testController.update(1)(FakeRequest().withBody(testUpdatedOrgAccountJson))
      await(res)

      status(res) mustBe OK
    }
  }

  lazy val mockGroupAccountConnector = mock[CustomerManagementApi]

  lazy val mockBrAuth = mock[BusinessRatesAuthConnector]


  lazy val testController = new GroupAccountController(preAuthenticatedActionBuilders(), mock[AuditingService], mockGroupAccountConnector, mockBrAuth)

}

