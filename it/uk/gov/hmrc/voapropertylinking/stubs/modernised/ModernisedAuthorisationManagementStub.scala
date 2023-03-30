package uk.gov.hmrc.voapropertylinking.stubs.modernised


import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.APIRepresentationResponse
import play.api.libs.json.JsValue
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ModernisedAuthorisationManagementStub extends WiremockMethods with WiremockHelper {


  def stubValidateAgentCode(agentCode: Long, authorisationId: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/authorisation-management-api/agent/validate_agent_code\\?agentCode=$agentCode&authorisationId=$authorisationId"
    ).thenReturn(status, body)

  def stubResponse(request: APIRepresentationResponse)(resp: HttpResponse): StubMapping = {
    when(
      method = PUT,
      uri = "/authorisation-management-api/agent/submit_agent_rep_reponse",
      body = request
    ).thenReturn(resp.status, resp.json)
  }
}
