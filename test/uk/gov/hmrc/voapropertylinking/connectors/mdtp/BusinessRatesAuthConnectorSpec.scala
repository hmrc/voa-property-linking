/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.mdtp

import basespecs.BaseUnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class BusinessRatesAuthConnectorSpec extends BaseUnitSpec {

  val connector: BusinessRatesAuthConnector =
    new BusinessRatesAuthConnector(mockDefaultHttpClient, mockServicesConfig) {
      override lazy val baseUrl: String = "http://some-uri"
    }

  "BusinessRatesAuthConnector clear cache" should {
    "delete the cache" in {
      when(mockDefaultHttpClient.DELETE[HttpResponse](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(200)))

      connector.clearCache().futureValue shouldBe ((): Unit)
    }
  }

}
