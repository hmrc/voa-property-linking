package uk.gov.hmrc.voapropertylinking.connectors.modernised

import models.APIRepresentationResponse
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsNull, JsObject, JsResultException, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.stubs.modernised.ModernisedAuthorisationManagementStub

import scala.concurrent.ExecutionContext

class ModernisedAuthorisationManagementApiISpec extends BaseIntegrationSpec with ModernisedAuthorisationManagementStub {

  trait TestSetup {
    lazy val connector: ModernisedAuthorisationManagementApi = app.injector.instanceOf[ModernisedAuthorisationManagementApi]
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
  }

  val agentId: Long = 7777777L
  val authorisationId: Long = 888888L
  val organisationId = 1234567L

  "validateAgentCode" should {
    "return an organisation ID" when {
      "it receives a valid response body" in new TestSetup {
        val validAgentCodeResponse: JsObject =
          Json.obj(
            "isValid" -> true,
            "organisationId" -> organisationId
          )
        stubValidateAgentCode(agentId, authorisationId)(OK, validAgentCodeResponse)

        val result: Either[Long, String] = await(connector.validateAgentCode(agentId, authorisationId))

        result shouldBe Left(organisationId)
      }
    }
    "return an organisation ID and ignore the failure code" when {
      "it receives a valid response body and a failure code" in new TestSetup {
        val validResponseWithFailureCode: JsObject =
          Json.obj(
            "isValid" -> true,
            "organisationId" -> organisationId,
            "failureCode" -> "NO_AGENT_FLAG"
          )
        stubValidateAgentCode(agentId, authorisationId)(OK, validResponseWithFailureCode)

        val result: Either[Long, String] = await(connector.validateAgentCode(agentId, authorisationId))

        result shouldBe Left(organisationId)
      }
    }
    "return an INVALID_CODE failure code" when {
      "it receives an invalid response body and a NO_AGENT_FLAG failure code" in new TestSetup {
        val invalidAgentCodeResponse: JsObject =
          Json.obj(
            "isValid" -> false,
            "organisationId" -> organisationId,
            "failureCode" -> "NO_AGENT_FLAG"
          )
        stubValidateAgentCode(agentId, authorisationId)(OK, invalidAgentCodeResponse)

        val result: Either[Long, String] = await(connector.validateAgentCode(agentId, authorisationId))

        result shouldBe Right("INVALID_CODE")
      }
    }
    "return an failure code" when {
      "it receives an invalid response body and any other failure code" in new TestSetup {
        val failureCode: String = "ANY_FAILURE"
        val invalidResponse: JsObject =
          Json.obj(
            "isValid" -> false,
            "organisationId" -> organisationId,
            "failureCode" -> failureCode
          )
        stubValidateAgentCode(agentId, authorisationId)(OK, invalidResponse)

        val result: Either[Long, String] = await(connector.validateAgentCode(agentId, authorisationId))

        result shouldBe Right(failureCode)
      }
    }

    "throw an exception" when {
      "the json response does not match the expected body" in new TestSetup {
        stubValidateAgentCode(agentId, authorisationId)(OK, Json.obj("not" -> "valid"))

        assertThrows[JsResultException]{
          await(connector.validateAgentCode(agentId, authorisationId))
        }
      }
      "it receives a valid response body but an 4xx HTTP status" in new TestSetup {
        val validAgentCodeResponse: JsObject =
          Json.obj(
            "isValid" -> true,
            "organisationId" -> organisationId
          )
        stubValidateAgentCode(agentId, authorisationId)(NOT_FOUND, validAgentCodeResponse)

        val result: Exception = intercept[VoaClientException] {
          await(connector.validateAgentCode(agentId, authorisationId))
        }

        result.getMessage shouldBe s"$validAgentCodeResponse"
      }
      "it receives a valid response body but an 5xx HTTP status" in new TestSetup {
        val validAgentCodeResponse: JsObject =
          Json.obj(
            "isValid" -> true,
            "organisationId" -> organisationId
          )
        stubValidateAgentCode(agentId, authorisationId)(INTERNAL_SERVER_ERROR, validAgentCodeResponse)

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse] {
          await(connector.validateAgentCode(agentId, authorisationId))
        }

        result.statusCode shouldBe INTERNAL_SERVER_ERROR
        result.getMessage should include(s"$validAgentCodeResponse")
      }
    }
  }

  "response" should {
    "return an 200 (OK) response" when {
      "supplied with the correct model" in new TestSetup {
        val request: APIRepresentationResponse = APIRepresentationResponse(
          submissionId = "abc123",
          authorisedPartyPersonId = 24680,
          outcome = "ok"
        )
        val response: HttpResponse = HttpResponse(OK, Json.obj("any" -> "body"), Map.empty)
        stubResponse(request)(response)

        val result: HttpResponse = await(connector.response(request))

        result.status shouldBe response.status
        result.json shouldBe Json.obj("any" -> "body")
      }
    }
    "return an error" when {
      "the http client returns an error" in new TestSetup {
        val request: APIRepresentationResponse = APIRepresentationResponse(
          submissionId = "abc123",
          authorisedPartyPersonId = 24680,
          outcome = "ok"
        )
        val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, JsNull, Map.empty)
        stubResponse(request)(response)

        val result: UpstreamErrorResponse = intercept[UpstreamErrorResponse]{
          await(connector.response(request))
        }

        result.statusCode shouldBe INTERNAL_SERVER_ERROR
        result.getMessage should include(s"${response.body}")
      }
    }
  }
}
