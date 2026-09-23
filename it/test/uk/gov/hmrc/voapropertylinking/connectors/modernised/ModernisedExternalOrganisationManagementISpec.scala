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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, postRequestedFor, urlEqualTo, verify}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException}
import uk.gov.hmrc.voapropertylinking.WiremockHelper.{wiremockHost, wiremockPort}
import uk.gov.hmrc.voapropertylinking.{BaseIntegrationSpec, WiremockMethods}
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesDashboardFrontendConnector
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation._
import uk.gov.hmrc.voapropertylinking.stubs.modernised.ModernisedExternalOrganisationManagementStub

import scala.concurrent.ExecutionContext

class ModernisedExternalOrganisationManagementISpec
    extends BaseIntegrationSpec with ModernisedExternalOrganisationManagementStub with WiremockMethods {

  override def config: Map[String, String] =
    super.config ++ Map(
      "microservice.services.business-rates-dashboard-frontend.host"                       -> wiremockHost,
      "microservice.services.business-rates-dashboard-frontend.port"                       -> wiremockPort.toString,
      "microservice.services.business-rates-dashboard-frontend.agentHasClientsCacheSecret" -> "secret"
    )

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    val agentId: Long = 123456789L

    lazy val connector: ModernisedExternalOrganisationManagementApi =
      app.injector.instanceOf[ModernisedExternalOrganisationManagementApi]
    lazy val dashboardFrontendConnector: BusinessRatesDashboardFrontendConnector =
      app.injector.instanceOf[BusinessRatesDashboardFrontendConnector]

    def appointmentChangesRequestJson(agentId: Long): JsValue =
      Json.parse(s"""{
                    |  "agentRepresentativeCode" : $agentId,
                    |  "action": "REVOKE",
                    |  "scope"  : "LIST_YEAR",
                    |  "propertyLinks" : ["PL123FRED", "PL654CARL"],
                    |  "listYears": ["2017", "2023"]
                    |}""".stripMargin)

    def appointmentChangesRequestModel(agentId: Long): AppointmentChangesRequest =
      AppointmentChangesRequest(
        agentRepresentativeCode = agentId,
        action = AppointmentAction.REVOKE,
        scope = AppointmentScope.LIST_YEAR,
        propertyLinks = Some(List("PL123FRED", "PL654CARL")),
        listYears = Some(List("2017", "2023"))
      )

    def appointmentChangesResponseJson(apptChangeId: String): JsObject =
      Json.obj("agentAppointmentChangeId" -> apptChangeId)

    def expectedAppointmentChangeResponse(apptChangeId: String): AppointmentChangeResponse =
      AppointmentChangeResponse(appointmentChangeId = apptChangeId)

    def stubInvalidateCache(agentCode: Long): Unit =
      when(
        POST,
        s"/business-rates-dashboard/internal/cache/agentHasClientsCache/$agentCode/invalidate/",
        Map("X-Cache-Endpoint-Secret" -> "secret")
      ).thenReturn(OK)
  }

  "getAgentDetails" should {
    val agentId = 123456789L
    "return the correct model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse("""
                                               |{
                                               | "name": "Super Agent",
                                               | "address": "123 Super Agent Street, AA1 1AA"
                                               |}
                                               |""".stripMargin)
      val agentDetailsModel: AgentDetails =
        agentrepresentation.AgentDetails(name = "Super Agent", address = "123 Super Agent Street, AA1 1AA")

      stubGetAgentDetails(agentId)(OK, responseJson)
      val result: Option[AgentDetails] = await(connector.getAgentDetails(agentId))

      result shouldBe Some(agentDetailsModel)
    }
    "throw an exception" when {
      "incorrect Json is received" in new TestSetup {
        stubGetAgentDetails(agentId)(OK, Json.obj("incorrect" -> "body"))

        val result: Exception = intercept[Exception] {
          await(connector.getAgentDetails(agentId))
        }

        result shouldBe a[JsValidationException]
      }
      "any error status is received" in new TestSetup {
        stubGetAgentDetails(agentId)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getAgentDetails(agentId))
        }
      }
    }
  }
  "agentAppointmentChanges" should {
    "return a valid response for the complete request" in new TestSetup {
      val apptChangeId = "change-id"
      val requestJson = appointmentChangesRequestJson(agentId)
      val requestModel = appointmentChangesRequestModel(agentId)
      val responseJson = appointmentChangesResponseJson(apptChangeId)
      val expectedResponse = expectedAppointmentChangeResponse(apptChangeId)

      stubAgentAppointmentChanges(requestJson)(OK, responseJson)

      val result: AppointmentChangeResponse =
        await(connector.agentAppointmentChanges(requestModel))

      result shouldBe expectedResponse
    }

    "invalidate the cache for the agent when the request succeeds" in new TestSetup {
      val apptChangeId = "change-id"
      val requestJson = appointmentChangesRequestJson(agentId)
      val requestModel = appointmentChangesRequestModel(agentId)
      val responseJson = appointmentChangesResponseJson(apptChangeId)

      stubInvalidateCache(agentId)
      stubAgentAppointmentChanges(requestJson)(OK, responseJson)

      await(connector.agentAppointmentChanges(requestModel))

      verify(
        postRequestedFor(
          urlEqualTo(s"/business-rates-dashboard/internal/cache/agentHasClientsCache/$agentId/invalidate/")
        )
          .withHeader("X-Cache-Endpoint-Secret", equalTo("secret"))
      )
    }
  }

}
