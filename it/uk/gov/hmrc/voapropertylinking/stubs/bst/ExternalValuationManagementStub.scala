package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ExternalValuationManagementStub extends WiremockMethods with WiremockHelper {


  private val baseUrl = "/external-valuation-management-api"

  def stubGetDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations/$valuationId/files\\?propertyLinkId=$propertyLinkId"
    ).thenReturn(status, body)
  }

  def stubGetValuationHistory(uarn: Long, propertyLinkId: String)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations\\?propertyLinkId=$propertyLinkId"
    ).thenReturn(status, body)
  }

  def stubGetDvrDocument(valuationId: Long,
                         uarn: Long,
                         propertyLinkId: String,
                         fileRef: String)(response: WSResponse)(implicit request: RequestWithPrincipal[_]): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations/$valuationId/files/$fileRef\\?propertyLinkId=$propertyLinkId",
      headers = Map(
        "GG-EXTERNAL-ID" -> request.principal.externalId,
        "GG-GROUP-ID" -> request.principal.groupId,
        "User-Agent" -> "voa-property-linking"
      )
    ).thenReturn(response.status, response.body)
  }
}
