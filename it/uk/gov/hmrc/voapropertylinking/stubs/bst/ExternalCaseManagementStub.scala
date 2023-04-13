package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ExternalCaseManagementStub extends WiremockMethods with WiremockHelper {

  private val baseUrl = "/external-case-management-api/my-organisation"
  def stubGetMyOrganisationCheckCases(propertyLinkSubmissionId: String)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/property-links/$propertyLinkSubmissionId/check-cases\\?start=1&size=100"
    ).thenReturn(status, body)
  }

  def stubGetMyClientsCheckCases(propertyLinkSubmissionId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/clients/all/property-links/$propertyLinkSubmissionId/check-cases\\?start=1&size=100"
    ).thenReturn(status, body)

  def stubCanChallengeClient(propertyLinkSubmissionId: String,
                             checkCaseRef: String,
                             valuationId: Long)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge\\?valuationId=$valuationId"
    ).thenReturn(status, body)
  }

  def stubCanChallengeAgent(propertyLinkSubmissionId:
                            String, checkCaseRef: String,
                            valuationId: Long)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/clients/all/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge\\?valuationId=$valuationId"
    ).thenReturn(status, body)
  }

}
