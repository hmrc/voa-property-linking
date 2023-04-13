package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.matching.StringValuePattern
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.PaginationParams
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ExternalPropertyLinkingStub extends WiremockMethods with WiremockHelper {


  private val baseUrl = "/external-property-link-management-api"
  def stubGetMyAgentPropertyLinks(agentCode: Long,
                                  searchParams: GetMyOrganisationPropertyLinksParameters,
                                  params: PaginationParams
                                 )(status: Int, body: JsValue): StubMapping = {

    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/agents/$agentCode/property-links"))
      .withQueryParam("start", equalTo(params.startPoint.toString))
      .withQueryParam("size", equalTo(params.pageSize.toString))
      .withQueryParam("requestTotalRowCount", equalTo("true"))
      .withQueryParam("address", optionEqualTo(searchParams.address))
      .withQueryParam("uarn", optionEqualTo(searchParams.uarn))
      .withQueryParam("baref", optionEqualTo(searchParams.baref))
      .withQueryParam("agent", optionEqualTo(searchParams.agent))
      .withQueryParam("status", optionEqualTo(searchParams.status))
      .withQueryParam("sortfield", optionEqualTo(searchParams.sortField))
      .withQueryParam("sortorder", optionEqualTo(searchParams.sortOrder))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGetMyOrganisationsPropertyLinks(searchParams: GetMyOrganisationPropertyLinksParameters,
                                          params: PaginationParams
                                         )(status: Int, body: JsValue): StubMapping = {

    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/property-links"))
      .withQueryParam("start", equalTo(params.startPoint.toString))
      .withQueryParam("size", equalTo(params.pageSize.toString))
      .withQueryParam("requestTotalRowCount", equalTo("true"))
      .withQueryParam("address", optionEqualTo(searchParams.address))
      .withQueryParam("uarn", optionEqualTo(searchParams.uarn))
      .withQueryParam("baref", optionEqualTo(searchParams.baref))
      .withQueryParam("agent", optionEqualTo(searchParams.agent))
      .withQueryParam("status", optionEqualTo(searchParams.status))
      .withQueryParam("sortfield", optionEqualTo(searchParams.sortField))
      .withQueryParam("sortorder", optionEqualTo(searchParams.sortOrder))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGetMyAgentAvailablePropertyLinks(agentCode: Long,
                                           searchParams: GetMyOrganisationPropertyLinksParameters,
                                           params: PaginationParams
                                          )(status: Int, body: JsValue): StubMapping = {

    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/agents/$agentCode/available-property-links"))
      .withQueryParam("start", equalTo(params.startPoint.toString))
      .withQueryParam("size", equalTo(params.pageSize.toString))
      .withQueryParam("requestTotalRowCount", equalTo("true"))
      .withQueryParam("address", optionEqualTo(searchParams.address))
      .withQueryParam("agent", optionEqualTo(searchParams.agent))
      .withQueryParam("sortfield", optionEqualTo(searchParams.sortField))
      .withQueryParam("sortorder", optionEqualTo(searchParams.sortOrder))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGetMyOrganisationsAgents()(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/my-organisation/agents\\?requestTotalRowCount=true"
    ).thenReturn(status, body)
  }
  def stubGetMyOrganisationsPropertyLink(submissionId: String)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/my-organisation/property-links/$submissionId"
    ).thenReturn(status, body)
  }

  def stubGetClientsPropertyLinks(searchParams: GetMyClientsPropertyLinkParameters,
                                  params: PaginationParams
                                 )(status: Int, body: JsValue): StubMapping = {

    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/clients/all/property-links"))
      .withQueryParam("start", equalTo(params.startPoint.toString))
      .withQueryParam("size", equalTo(params.pageSize.toString))
      .withQueryParam("requestTotalRowCount", equalTo("true"))
      .withQueryParam("address", optionEqualTo(searchParams.address))
      .withQueryParam("baref", optionEqualTo(searchParams.baref))
      .withQueryParam("client", optionEqualTo(searchParams.client))
      .withQueryParam("status", optionEqualTo(searchParams.status))
      .withQueryParam("sortfield", optionEqualTo(searchParams.sortField))
      .withQueryParam("sortorder", optionEqualTo(searchParams.sortOrder))
      .withQueryParam("representationStatus", optionEqualTo(searchParams.representationStatus))
      .withQueryParam("appointedFromDate", optionEqualTo(searchParams.appointedFromDate))
      .withQueryParam("appointedToDate", optionEqualTo(searchParams.appointedToDate))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGetClientPropertyLinks(clientOrgId: Long,
                                 searchParams: GetClientPropertyLinksParameters,
                                 params: PaginationParams
                                )(status: Int, body: JsValue): StubMapping = {

    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/clients/$clientOrgId/property-links"))
      .withQueryParam("start", equalTo(params.startPoint.toString))
      .withQueryParam("size", equalTo(params.pageSize.toString))
      .withQueryParam("requestTotalRowCount", equalTo("true"))
      .withQueryParam("address", optionEqualTo(searchParams.address))
      .withQueryParam("baref", optionEqualTo(searchParams.baref))
      .withQueryParam("status", optionEqualTo(searchParams.status))
      .withQueryParam("sortfield", optionEqualTo(searchParams.sortField))
      .withQueryParam("sortorder", optionEqualTo(searchParams.sortOrder))
      .withQueryParam("representationStatus", optionEqualTo(searchParams.representationStatus))
      .withQueryParam("appointedFromDate", optionEqualTo(searchParams.appointedFromDate))
      .withQueryParam("appointedToDate", optionEqualTo(searchParams.appointedToDate))
      .withQueryParam("client", optionEqualTo(searchParams.client))
      .withQueryParam("uarn", optionEqualTo(searchParams.uarn))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGetClientsPropertyLink(submissionId: String)(status: Int, body: JsValue): StubMapping = {
    when(
      method = GET,
      uri = s"$baseUrl/my-organisation/clients/all/property-links/$submissionId"
    ).thenReturn(status, body)
  }

  def stubGetMyClients(searchParams: GetClientsParameters, params: PaginationParams)(status: Int, body: JsValue): StubMapping = {
    stubFor(get(urlPathEqualTo(s"$baseUrl/my-organisation/clients"))
      .withQueryParam("name", optionEqualTo(searchParams.name))
      .withQueryParam("appointedFromDate", optionEqualTo(searchParams.appointedFromDate))
      .withQueryParam("appointedToDate", optionEqualTo(searchParams.appointedToDate))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubCreatePropertyLink(propertyLink: JsValue)(httpResponse: HttpResponse): StubMapping = {
    when(
      method = POST,
      uri = s"$baseUrl/my-organisation/property-links",
      body = propertyLink
    ).thenReturn(httpResponse.status, httpResponse.json)
  }

  def stubCreateOnClientBehalf(propertyLink: JsValue, clientId: Long)(httpResponse: HttpResponse): StubMapping = {
    when(
      method = POST,
      uri = s"$baseUrl/my-organisation/clients/$clientId/property-links",
      body = propertyLink
    ).thenReturn(httpResponse.status, httpResponse.json)
  }

  def stubRevokeClientProperty(plSubmissionId: String)(httpResponse: HttpResponse): StubMapping = {
    when(
      method = DELETE,
      uri = s"$baseUrl/my-organisation/clients/all/property-links/$plSubmissionId/appointment"
    ).thenReturn(httpResponse.status, httpResponse.json)
  }

  private def optionEqualTo[A](opt: Option[A]): StringValuePattern =
    equalTo(opt.getOrElse("NO_PARAMETER_SUPPLIED").toString)
}
