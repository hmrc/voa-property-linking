/*
 * Copyright 2019 HM Revenue & Customs
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

package connectors

import java.time.{Clock, Instant, ZoneId}

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class AddressConnectorSpec extends ContentTypes with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val testConnector = new AddressConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl = mockServerUrl
  }

  val url = s"/address-management-api/address"

  "AddressConnector.get" should {
    "return the address associated with the address unit id" in {
      val addressUnitId = 1234L
      val getUrl = s"$url/$addressUnitId"
      val stub = stubFor(get(urlEqualTo(getUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.get(addressUnitId)(hc))) shouldBe expectedGetValidResponse
    }
  }

  // Detailed Address
  lazy val getResponseValid = """{
      "addressDetails": [
        {
          |"addressUnitId": 123456789,
          |"nonAbpAddressId": 1234,
          |"organisationName": "Liverpool FC",
          |"departmentName": "First team",
          |"buildingName": "Anfield Stadium",
          |"dependentThoroughfareName": "Anfield Road",
          |"postTown": "Liverpool",
          |"postcode": "L4 0TH"
        }
      ]
    }""".stripMargin

  lazy val expectedGetValidResponse = Some(SimpleAddress(
    addressUnitId = Some(123456789),
    line1 = "Liverpool FC, First team",
    line2 = "Anfield Stadium",
    line3 = "Anfield Road",
    line4 = "Liverpool",
    postcode = "L4 0TH")
  )

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
}
