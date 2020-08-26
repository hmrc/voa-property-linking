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
        auditingService = mockAuditingService,
        externalPropertyLinkApi = mockExternalPropertyLinkApi
      )
    protected val submissionId = "PL123"
    protected val agentCode = 12345L
    protected val authorisationId = 54321L
    protected val orgId = 1L
    protected val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = false)
    protected val ownerAuthResult = ModernisedOwnerAuthResult(1, 1, 1, 1, Seq())

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

  "revoke client property" should {
    "return 204 NoContent" when {
      "property link submission id is provided" in new Setup {
        when(mockExternalPropertyLinkApi.revokeClientProperty(any())(any()))
          .thenReturn(Future.successful(()))

        val result: Future[Result] =
          testController.revokeClientProperty("some-sumissionId")(FakeRequest())

        status(result) shouldBe NO_CONTENT
        verify(mockExternalPropertyLinkApi).revokeClientProperty(any())(any())
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

  "assignAgent" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.assignAgent()(FakeRequest().withBody(Json.parse("""{
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
          testController.assignAgent()(FakeRequest().withBody(Json.parse("""{
                                                                           |  "agentRepresentativeCode" : 1
                                                                           |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "assignAgentToSomeProperties" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.assignAgentToSomeProperties()(
            FakeRequest().withBody(Json.parse("""{
                                                |  "agentCode" : 1,
                                                |  "propertyLinkIds"  : ["PL123BLAH", "PL654BLUH"]
                                                |}""".stripMargin)))

        status(result) shouldBe ACCEPTED
      }
    }
    "return 400 Bad Request" when {
      "invalid request is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.assignAgentToSomeProperties()(FakeRequest().withBody(Json.parse("""{
                                                                                           |  "agentCode" : 1
                                                                                           |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "unassignAgent" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.unassignAgent()(FakeRequest().withBody(Json.parse("""{
                                                                             |  "agentRepresentativeCode" : 123,
                                                                             |  "scope"  : "ALL_PROPERTIES"
                                                                             |}""".stripMargin)))

        status(result) shouldBe ACCEPTED
      }
    }
    "return 400 Bad Request" when {
      "invalid unassign agent request is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.unassignAgent()(FakeRequest().withBody(Json.parse("""{
                                                                             |  "agentRepresentativeCode" : 1
                                                                             |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "unassignAgentFromSomeProperties" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.unassignAgentFromSomeProperties()(
            FakeRequest().withBody(Json.parse("""{
                                                |  "agentCode" : 1,
                                                |  "propertyLinkIds"  : ["PL123BLAH", "PL654BLUH"]
                                                |}""".stripMargin)))

        status(result) shouldBe ACCEPTED
      }
    }
    "return 400 Bad Request" when {
      "invalid request is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.unassignAgentFromSomeProperties()(
            FakeRequest().withBody(Json.parse("""{
                                                |  "propertyLinkIds"  : ["PL123BLAH", "PL654BLUH"]
                                                |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  "removeAgentFromOrganisation" should {
    "return 202 Accepted" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
          .thenReturn(Future.successful(appointmentChangeResponse))

        val result: Future[Result] =
          testController.removeAgentFromOrganisation()(
            FakeRequest().withBody(Json.parse("""{
                                                |  "agentRepresentativeCode" : 123,
                                                |  "scope"  : "RELATIONSHIP"
                                                |}""".stripMargin)))

        status(result) shouldBe ACCEPTED
      }
    }
    "return 400 Bad Request" when {
      "invalid removeAgentFromOrganisation agent request is POSTed" in new Setup {

        val result: Future[Result] =
          testController.removeAgentFromOrganisation()(
            FakeRequest().withBody(Json.parse("""{
                                                |  "agentRepresentativeCode" : 1
                                                |}""".stripMargin)))

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

}
