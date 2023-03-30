package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.modernised.addressmanagement.DetailedAddress
import play.api.libs.json.JsValue
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait AddressManagementStub extends WiremockMethods with WiremockHelper {

  def stubFind(postcode: String)(status: Int, body: JsValue): StubMapping = {
    stubFor(get(urlPathEqualTo("/address-management-api/address"))
      .withQueryParam("pageSize", equalTo("100"))
      .withQueryParam("startPoint", equalTo("1"))
      .withQueryParam("searchparams", containing("postcode"))
      .withQueryParam("searchparams", containing(s"$postcode"))
      .willReturn(
        aResponse().withStatus(status).withBody(body.toString)
      )
    )
  }

  def stubGet(addressUnitId: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/address-management-api/address/$addressUnitId"
    ).thenReturn(status, body)

  def stubCreate(address: DetailedAddress)(status: Int, body: JsValue): StubMapping = {
    when(
      method = POST,
      uri = s"/address-management-api/address/non_standard_address",
      body = address
    ).thenReturn(status, body)
  }

}
