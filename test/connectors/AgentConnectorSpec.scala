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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import helpers.SimpleWsHttpTestApplication
import play.api.http.ContentTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class AgentConnectorSpec extends ContentTypes
  with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()

  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new AgentConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl: String = mockServerUrl
  }

  "AgentConnector manage agents" should {
    "find owner agents" in {

      val organisationId = 123
      val manageAgentsUrl = s"/authorisation-search-api/owners/$organisationId/agents"

      stubFor(get(urlEqualTo(manageAgentsUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(ownerAgents)
        )
      )

      await(connector.manageAgents(organisationId)(hc)).agents.size shouldBe 2
    }
  }

  val ownerAgents =
    """
      |{
      |  "agents": [
      |   {
      |     "name": "Test name 1",
      |     "ref": 123
      |   },
      |   {
      |     "name": "Test name 2",
      |     "ref": 123
      |   }
      |  ]
      |}
    """.stripMargin

}
