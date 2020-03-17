/*
 * Copyright 2020 HM Revenue & Customs
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

import basespecs.BaseControllerSpec
import models.searchApi.{OwnerAuthResult => ModernisedOwnerAuthResult}
import models.{APIRepresentationResponse, GroupAccount, PaginationParams}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AgentDetails

import scala.concurrent.Future

class PropertyRepresentationControllerSpec extends BaseControllerSpec {

  trait Setup {
    val testController: PropertyRepresentationController =
      new PropertyRepresentationController(
        controllerComponents = Helpers.stubControllerComponents(),
        authenticated = preAuthenticatedActionBuilders(),
        authorisationManagementApi = mockAuthorisationManagementApi,
        authorisationSearchApi = mockAuthorisationSearchApi,
        customerManagementApi = mockCustomerManagementApi,
        organisationManagementApi = mockOrganisationManagementApi,
        auditingService = mockAuditingService
      )
    protected val submissionId = "PL123"
    protected val agentCode = 12345L
    protected val authorisationId = 54321L
    protected val orgId = 1L
    protected val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = false)
    protected val ownerAuthResult = ModernisedOwnerAuthResult(1, 1, 1, 1, Seq())

  }

  "create" should {
    "return 200 OK" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockAuthorisationManagementApi.create(any())(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        val result: Future[Result] =
          testController.create()(FakeRequest().withBody(Json.parse("""{
                                                                      |  "authorisationId" : 1,
                                                                      |  "agentOrganisationId" : 2,
                                                                      |  "individualId" : 3,
                                                                      |  "submissionId" : "A1",
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

  "response" should {
    "return OK 200" when {
      "a valid representation response is POSTed" in new Setup {
        val repResp = APIRepresentationResponse(submissionId, 1L, "OUTCOME")
        when(mockAuthorisationManagementApi.response(mEq(repResp))(any()))
          .thenReturn(Future.successful(HttpResponse(OK)))

        val result: Future[Result] =
          testController.response()(FakeRequest().withBody(Json.toJson(repResp)))

        status(result) shouldBe OK

        verify(mockAuditingService).sendEvent(mEq("agent representation response"), mEq(repResp))(any(), any(), any())
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
        when(mockAuthorisationSearchApi.appointableToAgent(any(), any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(ownerAuthResult))

        val result: Future[Result] =
          testController.appointableToAgent(
            ownerId = 1L,
            agentCode = agentCode,
            paginationParams = paginationParams,
            sortfield = None,
            sortorder = None,
            address = None,
            agent = None)(FakeRequest())

        status(result) shouldBe OK
      }
    }

    "return NOT FOUND 404" when {
      "customer management API returns nothing for given agent code" in new Setup {
        when(mockCustomerManagementApi.withAgentCode(mEq(agentCode.toString))(any()))
          .thenReturn(Future.successful(Option.empty[GroupAccount]))

        val result: Future[Result] =
          testController.appointableToAgent(
            ownerId = 1L,
            agentCode = agentCode,
            paginationParams = paginationParams,
            sortfield = None,
            sortorder = None,
            address = None,
            agent = None)(FakeRequest())

        status(result) shouldBe NOT_FOUND

        verify(mockAuthorisationSearchApi, never())
          .appointableToAgent(any(), any(), any(), any(), any(), any(), any())(any())
      }
    }

  }

  "getAgentDetails" should {
    "return OK 200" when {
      "organisation management API returns AgentDetails for a provided agent code" in new Setup {
        when(mockOrganisationManagementApi.getAgentDetails(mEq(agentCode))(any(), any()))
          .thenReturn(Future.successful(Some(agentDetails)))

        val result: Future[Result] =
          testController.getAgentDetails(agentCode)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.toJson(agentDetails)
      }
    }

    "return NOT FOUND 404" when {
      "organisation management API returns nothing for given agent code" in new Setup {
        when(mockOrganisationManagementApi.getAgentDetails(mEq(agentCode))(any(), any()))
          .thenReturn(Future.successful(Option.empty[AgentDetails]))

        val result: Future[Result] =
          testController.getAgentDetails(agentCode)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }

  }

  "appointAgent" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.appointAgent()(FakeRequest().withBody(Json.parse("""{
                                                                            |  "agentRepresentativeCode" : 1,
                                                                            |  "scope"  : "RELATIONSHIP"
                                                                            |}""".stripMargin)))

        status(result) shouldBe ACCEPTED
      }
    }
    "return 400 Bad Request" when {
      "invalid appoint agent request is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.appointAgent()(FakeRequest().withBody(Json.parse("""{
                                                                            |  "agentRepresentativeCode" : 1
                                                                            |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

}
