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

package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.modernised.addressmanagement.DetailedAddress
import play.api.libs.json.JsValue
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait AddressManagementStub extends WiremockMethods with WiremockHelper {

  def stubFind(postcode: String)(status: Int, body: JsValue): StubMapping =
    stubFor(
      get(urlPathEqualTo("/address-management-api/address"))
        .withQueryParam("pageSize", equalTo("100"))
        .withQueryParam("startPoint", equalTo("1"))
        .withQueryParam("searchparams", containing("postcode"))
        .withQueryParam("searchparams", containing(s"$postcode"))
        .willReturn(
          aResponse().withStatus(status).withBody(body.toString)
        )
    )

  def stubGet(addressUnitId: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"/address-management-api/address/$addressUnitId"
    ).thenReturn(status, body)

  def stubCreate(address: DetailedAddress)(status: Int, body: JsValue): StubMapping =
    when(
      method = POST,
      uri = s"/address-management-api/address/non_standard_address",
      body = address
    ).thenReturn(status, body)

}
