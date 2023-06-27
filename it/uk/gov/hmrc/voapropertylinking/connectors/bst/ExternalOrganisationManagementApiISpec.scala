package uk.gov.hmrc.voapropertylinking.connectors.bst

import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.{AgentDetails, AppointmentAction, AppointmentChangeResponse, AppointmentChangesRequest, AppointmentScope, AssignAgent}
import uk.gov.hmrc.voapropertylinking.stubs.bst.ExternalOrganisationManagementStub

import scala.concurrent.ExecutionContext

class ExternalOrganisationManagementApiISpec extends BaseIntegrationSpec with ExternalOrganisationManagementStub {

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    lazy val connector: ExternalOrganisationManagementApi = app.injector.instanceOf[ExternalOrganisationManagementApi]
  }

  "agentAppointmentChanges" should {
    val agentId = 123456789L
    val requestJson: JsValue =
      Json.parse(
        s"""{
           |  "agentRepresentativeCode" : $agentId,
           |  "action": "APPOINT",
           |  "scope"  : "RELATIONSHIP"
           |}""".stripMargin)
    val requestModel = AppointmentChangesRequest.apply(AssignAgent(agentId, "RELATIONSHIP"))

      "return a valid responseModel" in new TestSetup {
        val apptChangeId = "change-id"
        val responseJson: JsObject = Json.obj(
          "agentAppointmentChangeId" -> apptChangeId
        )
        val expectedResponseModel: AppointmentChangeResponse = AppointmentChangeResponse(appointmentChangeId = apptChangeId)

        stubAgentAppointmentChanges(requestJson)(OK, responseJson)

        val result: AppointmentChangeResponse = await(connector.agentAppointmentChanges(requestModel))

        result shouldBe expectedResponseModel
      }
    "return an exception" when {
      "it receives a downstream 4xx response" in new TestSetup {
        stubAgentAppointmentChanges(requestJson)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.agentAppointmentChanges(requestModel))        }

        result shouldBe a[VoaClientException]
      }
      "it receives a downstream 5xx response" in new TestSetup {
        stubAgentAppointmentChanges(requestJson)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.agentAppointmentChanges(requestModel))        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

  "getAgentDetails" should {
    val agentId = 123456789L
    "return the correct model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        """
          |{
          | "name": "Super Agent",
          | "address": "123 Super Agent Street, AA1 1AA"
          |}
          |""".stripMargin)
      val agentDetailsModel: AgentDetails = agentrepresentation.AgentDetails(name = "Super Agent", address = "123 Super Agent Street, AA1 1AA")

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
    "return a valid response for the complete request " in new TestSetup {

      val agentId = 123456789L
      val requestJson: JsValue =
        Json.parse(
          s"""{
             |  "agentRepresentativeCode" : $agentId,
             |  "action": "APPOINT",
             |  "scope"  : "LIST_YEAR",
             |  "propertyLinks" : ["PL123FRED", "PL654CARL"],
             |  "listYears": ["2017", "2023"]
             |}""".stripMargin)

      val requestModel = AppointmentChangesRequest(
        agentRepresentativeCode = agentId,
        action = AppointmentAction.APPOINT,
        scope = AppointmentScope.LIST_YEAR,
        propertyLinks = Some(List("PL123FRED", "PL654CARL")),
        listYears = Some(List("2017", "2023"))
      )

      val apptChangeId = "change-id"
      val responseJson: JsObject = Json.obj(
        "agentAppointmentChangeId" -> apptChangeId
      )
      val expectedResponse: AppointmentChangeResponse = AppointmentChangeResponse(appointmentChangeId = apptChangeId)

      stubAgentAppointmentChanges(requestJson)(OK, responseJson)

      val result: AppointmentChangeResponse = await(connector.agentAppointmentChanges(requestModel))

      result shouldBe expectedResponse
    }
  }

}
