package uk.gov.hmrc.voapropertylinking.stubs.modernised

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ModernisedExternalOrganisationManagementStub extends WiremockMethods with WiremockHelper {

  def stubAgentAppointmentChanges(appointmentChangesRequest: JsValue)(status: Int, body: JsValue): StubMapping = {
    when(
      method = POST,
      uri = s"/external-organisation-management-api/my-organisation/agentAppointmentChanges",
      body = appointmentChangesRequest
    ).thenReturn(status, body)
  }

  def stubGetAgentDetails(agentCode: Long)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"/external-organisation-management-api/agents/$agentCode"
    ).thenReturn(status, body)
  }

}
