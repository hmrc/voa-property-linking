package uk.gov.hmrc.voapropertylinking.connectors.bst

import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.stubs.bst.CCACaseManagementStub

import scala.concurrent.ExecutionContext

class CCACaseManagementApiISpec extends BaseIntegrationSpec with CCACaseManagementStub {

  trait TestSetup {
    lazy val connector: CCACaseManagementApi = app.injector.instanceOf[CCACaseManagementApi]
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
  }

  "requestDetailedValuation" should {
    "return a unit" when {
      "it receives a 200 (OK) response" in new TestSetup {
        val request: DetailedValuationRequest = DetailedValuationRequest(
          authorisationId = 123456,
          organisationId = 9876543,
          personId = 1111111,
          submissionId = "submission1",
          assessmentRef = 24680,
          agents = None,
          billingAuthorityReferenceNumber = "barn1"
        )
        val response: HttpResponse = HttpResponse(OK, Json.obj("any" -> "body"), Map.empty)
        stubRequestDetailedValuation(request)(response)

        val result: Unit = await(connector.requestDetailedValuation(request))

        result shouldBe ()
      }
    }
    "throw an exception" when {
      "it receives any 4xx response" in new TestSetup {
        val request: DetailedValuationRequest = DetailedValuationRequest(
          authorisationId = 123456,
          organisationId = 9876543,
          personId = 1111111,
          submissionId = "submission1",
          assessmentRef = 24680,
          agents = None,
          billingAuthorityReferenceNumber = "barn1"
        )
        val response: HttpResponse = HttpResponse(BAD_REQUEST, Json.obj("any" -> "body"), Map.empty)
        stubRequestDetailedValuation(request)(response)

        val result: Exception = intercept[Exception]{
          await(connector.requestDetailedValuation(request))
        }

        result shouldBe a [UpstreamErrorResponse]
      }
      "it receives any 5xx response" in new TestSetup {
        val request: DetailedValuationRequest = DetailedValuationRequest(
          authorisationId = 123456,
          organisationId = 9876543,
          personId = 1111111,
          submissionId = "submission1",
          assessmentRef = 24680,
          agents = None,
          billingAuthorityReferenceNumber = "barn1"
        )
        val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.obj("any" -> "body"), Map.empty)
        stubRequestDetailedValuation(request)(response)

        val result: Exception = intercept[Exception] {
          await(connector.requestDetailedValuation(request))
        }

        result shouldBe a [UpstreamErrorResponse]
      }
    }
  }
}
