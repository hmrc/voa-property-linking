/*
 * Copyright 2018 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import helpers.SimpleWsHttpTestApplication
import models.DetailedValuationRequest
import play.api.http.ContentTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

class DVRCaseManagementConnectorSpec extends ContentTypes
  with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()

  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new DVRCaseManagementConnector(http) {
    override lazy val baseURL: String = mockServerUrl
  }

  "DVRCaseManagementConnector requestDetailedValuation" should {
    "update the valuation with the detailed valuation request" in {

      val dvrUrl = s"/dvr-case-management-api/dvr_case/create_dvr_case"

      stubFor(post(urlEqualTo(dvrUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(emptyCache)
        )
      )

      val dvr = DetailedValuationRequest(
        authorisationId = 123456,
        organisationId = 9876543,
        personId = 1111111,
        submissionId = "submission1",
        assessmentRef = 24680,
        billingAuthorityReferenceNumber = "barn1"
      )

      val result: Unit = await(connector.requestDetailedValuation(dvr))
      result shouldBe ()
    }
  }

  val emptyCache ="{}"

}
