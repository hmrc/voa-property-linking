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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, delete, stubFor, urlEqualTo}
import helpers.SimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig

class BusinessRatesAuthConnectorSpec extends ContentTypes
  with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()

  val http = fakeApplication.injector.instanceOf[SimpleWSHttp]
  val connector = new BusinessRatesAuthConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl: String = mockServerUrl
  }

  "BusinessRatesAuthConnector clear cache" should {
    "delete the cache" in {

      val clearCacheUrl = s"/business-rates-authorisation/cache"

      stubFor(delete(urlEqualTo(clearCacheUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(emptyCache)
        )
      )

      val result: Unit = await(connector.clearCache())
      result shouldBe ()
    }
  }

  val emptyCache ="{}"

}