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

import basespecs.BaseControllerSpec
import models.searchApi.{OwnerAuthResult => ModernisedOwnerAuthResult}
import models.PaginationParams
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AgentDetails

import scala.concurrent.Future

class PropertyRepresentationControllerSpec extends BaseControllerSpec {

  trait Setup {
    val testController: PropertyRepresentationController =
      new PropertyRepresentationController(
        controllerComponents = Helpers.stubControllerComponents(),
        authenticated = preAuthenticatedActionBuilders(),
        modernisedOrganisationManagementApi = mockModernisedOrganisationManagementApi,
        modernisedExternalPropertyLinkApi = mockModernisedExternalPropertyLinkApi,
        organisationManagementApi = mockOrganisationManagementApi,
        propertyLinkApi = mockPropertyLinkApi,
        featureSwitch = mockFeatureSwitch,
        auditingService = mockAuditingService
      )
    protected val submissionId = "PL123"
    protected val agentCode = 12345L
    protected val authorisationId = 54321L
    protected val orgId = 1L
    protected val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = false)
    protected val ownerAuthResult = ModernisedOwnerAuthResult(1, 1, 1, 1, Seq())

  }

  def calling: AfterWord = afterWord("calling")

  "If the bstDownstream feature switch is enabled" when {
    "revoke client property" should {
      "return 204 NoContent" when {
        "property link submission id is provided" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockPropertyLinkApi.revokeClientProperty(any())(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] =
            testController.revokeClientProperty("some-sumissionId")(FakeRequest())

          status(result) shouldBe NO_CONTENT
          verify(mockPropertyLinkApi).revokeClientProperty(any())(any())
        }
      }
    }

    "getAgentDetails" should {
      "return OK 200" when {
        "organisation management API returns AgentDetails for a provided agent code" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
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
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockOrganisationManagementApi.getAgentDetails(mEq(agentCode))(any(), any()))
            .thenReturn(Future.successful(Option.empty[AgentDetails]))

          val result: Future[Result] =
            testController.getAgentDetails(agentCode)(FakeRequest())

          status(result) shouldBe NOT_FOUND
        }
      }
    }

    "submitAppointmentChanges" should {
      "return 202 Accepted" when {
        "valid JSON payload is POSTed" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
            .thenReturn(Future.successful(appointmentChangeResponse))

          val result: Future[Result] =
            testController.submitAppointmentChanges()(
              FakeRequest().withBody(Json.parse("""{
                                                  |  "agentRepresentativeCode" : 123,
                                                  |  "action" : "APPOINT",
                                                  |  "scope"  : "LIST_YEAR",
                                                  |  "listYears": ["2017"]
                                                  |}""".stripMargin))
            )

          status(result) shouldBe ACCEPTED
        }
      }
      "return 400 Bad Request" when {
        "invalid removeAgentFromOrganisation agent request is POSTed" in new Setup {

          val result: Future[Result] =
            testController.submitAppointmentChanges()(
              FakeRequest().withBody(Json.parse("""{
                                                  |  "agentRepresentativeCode" : 1
                                                  |}""".stripMargin))
            )

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }

  "If the bstDownstream feature switch is disabled" when calling {
    "revoke client property" should {
      "return 204 NoContent" when {
        "property link submission id is provided" in new Setup {
          when(mockModernisedExternalPropertyLinkApi.revokeClientProperty(any())(any()))
            .thenReturn(Future.successful(()))

          val result: Future[Result] =
            testController.revokeClientProperty("some-sumissionId")(FakeRequest())

          status(result) shouldBe NO_CONTENT
          verify(mockModernisedExternalPropertyLinkApi).revokeClientProperty(any())(any())
        }
      }
    }

    "getAgentDetails" should {
      "return OK 200" when {
        "organisation management API returns AgentDetails for a provided agent code" in new Setup {
          when(mockModernisedOrganisationManagementApi.getAgentDetails(mEq(agentCode))(any(), any()))
            .thenReturn(Future.successful(Some(agentDetails)))

          val result: Future[Result] =
            testController.getAgentDetails(agentCode)(FakeRequest())

          status(result) shouldBe OK
          contentAsJson(result) shouldBe Json.toJson(agentDetails)
        }
      }

      "return NOT FOUND 404" when {
        "organisation management API returns nothing for given agent code" in new Setup {
          when(mockModernisedOrganisationManagementApi.getAgentDetails(mEq(agentCode))(any(), any()))
            .thenReturn(Future.successful(Option.empty[AgentDetails]))

          val result: Future[Result] =
            testController.getAgentDetails(agentCode)(FakeRequest())

          status(result) shouldBe NOT_FOUND
        }
      }
    }

    "submitAppointmentChanges" should {
      "return 202 Accepted" when {
        "valid JSON payload is POSTed" in new Setup {

          when(mockModernisedOrganisationManagementApi.agentAppointmentChanges(any())(any(), any()))
            .thenReturn(Future.successful(appointmentChangeResponse))

          val result: Future[Result] =
            testController.submitAppointmentChanges()(
              FakeRequest().withBody(Json.parse("""{
                                                  |  "agentRepresentativeCode" : 123,
                                                  |  "action" : "APPOINT",
                                                  |  "scope"  : "LIST_YEAR",
                                                  |  "listYears": ["2017"]
                                                  |}""".stripMargin))
            )

          status(result) shouldBe ACCEPTED
        }
      }
      "return 400 Bad Request" when {
        "invalid removeAgentFromOrganisation agent request is POSTed" in new Setup {

          val result: Future[Result] =
            testController.submitAppointmentChanges()(
              FakeRequest().withBody(Json.parse("""{
                                                  |  "agentRepresentativeCode" : 1
                                                  |}""".stripMargin))
            )

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
