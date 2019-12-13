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

import basespecs.BaseControllerSpec
import models.searchApi.{ OwnerAuthResult => ModernisedOwnerAuthResult }
import models.{APIRepresentationResponse, GroupAccount, PaginationParams, PropertyRepresentations}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class PropertyRepresentationControllerSpec extends BaseControllerSpec {

  trait Setup {
    val testController: PropertyRepresentationController =
      new PropertyRepresentationController(
        authenticated = preAuthenticatedActionBuilders(),
        authorisationManagementApi = mockAuthorisationManagementApi,
        authorisationSearchApi = mockAuthorisationSearchApi,
        customerManagementApi = mockCustomerManagementApi,
        auditingService = mockAuditingService
      )
    protected val submissionId = "PL123"
    protected val agentCode = 12345L
    protected val authorisationId = 54321L
    protected val orgId = 1L
    protected val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = false)
    protected val groupAccount = GroupAccount(
      id = 2,
      groupId = "gggId",
      companyName = "Fake News Inc",
      addressId = 345,
      email = "therealdonald@potus.com",
      phone = "9876541",
      isAgent = false,
      agentCode = Some(234)
    )
    protected val ownerAuthResult = ModernisedOwnerAuthResult(1, 1, 1, 1, Seq())

  }

  "create" should {
    "return 200 OK" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockAuthorisationManagementApi.create(any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        val result: Future[Result] = testController.create()(FakeRequest().withBody(Json.parse(
          """{
            |  "authorisationId" : 1,
            |  "agentOrganisationId" : 2,
            |  "individualId" : 3,
            |  "submissionId" : "A1",
            |  "checkPermission" : "START",
            |  "challengePermission" : "START",
            |  "createDatetime" : "2019-09-12T15:36:47.125Z"
            |}""".stripMargin)))

        status(result) shouldBe OK
      }
    }
  }

  "validateAgentCode" should {
    "return OK 200" when {
      "the agent code is valid" in new Setup {
        when(mockAuthorisationManagementApi.validateAgentCode(mEq(agentCode), mEq(authorisationId))(any()))
          .thenReturn(Future.successful(orgId.asLeft[String]))

        val result: Future[Result] = testController.validateAgentCode(agentCode, authorisationId)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.obj("organisationId" -> orgId)
      }

      "the agent code is NOT valid" in new Setup {
        when(mockAuthorisationManagementApi.validateAgentCode(mEq(agentCode), mEq(authorisationId))(any()))
          .thenReturn(Future.successful("ERROR".asRight[Long]))

        val result: Future[Result] = testController.validateAgentCode(agentCode, authorisationId)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.obj("failureCode" -> "ERROR")
      }

    }
  }

  "forAgent" should {
    "return OK 200" when {
      "there is a property representation" in new Setup {
        when(mockAuthorisationSearchApi.forAgent(any(), any(), any())(any()))
          .thenReturn(Future.successful(PropertyRepresentations(1, Seq.empty)))

        val result: Future[Result] = testController.forAgent("OPEN", orgId, paginationParams)(FakeRequest())

        status(result) shouldBe OK
      }
    }
  }

  "response" should {
    "return OK 200" when {
      "a valid representation response is POSTed" in new Setup {
        val repResp = APIRepresentationResponse(submissionId, 1L, "OUTCOME")
        when(mockAuthorisationManagementApi.response(mEq(repResp))(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        val result: Future[Result] =
          testController.response()(FakeRequest().withBody(Json.toJson(repResp)))

        status(result) shouldBe OK

        verify(mockAuditingService).sendEvent(mEq("agent representation response"), mEq(repResp))(any(), any(), any(), any())
      }
    }
  }

  "revoke" should {
    "return OK 200" when {
      "valid PATCH request is made with authorisedPartyId" in new Setup {
        val authorisedPartyId = 123L
        when(mockAuthorisationManagementApi.revoke(mEq(authorisedPartyId))(any()))
          .thenReturn(Future.successful(mock[HttpResponse]))

        val result: Future[Result] =
          testController.revoke(authorisedPartyId)(FakeRequest().withBody(Json.obj()))

        status(result) shouldBe OK
        verify(mockAuthorisationManagementApi).revoke(mEq(authorisedPartyId))(any())
      }
    }
  }

  "appointableToAgent" should {
    "return OK 200" when {
      "customer management API returns an agent group for specified agent code" in new Setup {
        when(mockCustomerManagementApi.withAgentCode(mEq(agentCode.toString))(any()))
          .thenReturn(Future.successful(Some(groupAccount)))
        when(mockAuthorisationSearchApi.appointableToAgent(any(), any(), any(), any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(ownerAuthResult))

        val result: Future[Result] =
          testController.appointableToAgent(1L, agentCode, None, None, paginationParams, None, None, None, None)(FakeRequest())

        status(result) shouldBe OK
      }
    }

    "return NOT FOUND 404" when {
      "customer management API returns nothing for given agent code" in new Setup {
        when(mockCustomerManagementApi.withAgentCode(mEq(agentCode.toString))(any()))
          .thenReturn(Future.successful(Option.empty[GroupAccount]))

        val result: Future[Result] =
          testController.appointableToAgent(1L, agentCode, None, None, paginationParams, None, None, None, None)(FakeRequest())

        status(result) shouldBe NOT_FOUND

        verify(mockAuthorisationSearchApi, never())
          .appointableToAgent(any(), any(), any(), any(), any(), any(), any(), any(), any())(any())
      }
    }

  }

}