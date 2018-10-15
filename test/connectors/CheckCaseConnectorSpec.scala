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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import helpers.SimpleWsHttpTestApplication
import models.ModernisedEnrichedRequest
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class CheckCaseConnectorSpec extends ContentTypes
  with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  implicit val modernisedEnrichedRequest = ModernisedEnrichedRequest(FakeRequest(), "XXXXX", "YYYYY")

  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new CheckCaseConnector(fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl: String = mockServerUrl
  }

  "CheckCaseConnector get check cases" should {
    "get agents check cases" in {

      val submissionId = 123
      val manageAgentsUrl = s"/external-case-management-api/my-organisation/clients/all/property-links/$submissionId/check-cases?start=1&size=15"

      stubFor(get(urlEqualTo(manageAgentsUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(agentAgents)
        )
      )

      await(connector.getCheckCases(submissionId, "agent")(modernisedEnrichedRequest)).get.filterTotal shouldBe 4
    }

    "get client check cases" in {

      val submissionId = 123
      val manageAgentsUrl = s"/external-case-management-api/my-organisation/property-links/$submissionId/check-cases?start=1&size=15"

      stubFor(get(urlEqualTo(manageAgentsUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(ownerAgents)
        )
      )

      await(connector.getCheckCases(submissionId, "client")(modernisedEnrichedRequest)).get.filterTotal shouldBe 4
    }
  }


  val agentAgents =
    """
      |{
      |  "start": 1,
      |  "size": 15,
      |  "filterTotal": 4,
      |  "total": 4,
      |  "checkCases": [
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW9Q",
      |      "checkCaseReference": "CHK100028783",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-31T08:16:54",
      |      "submittedBy": "OA Ltd",
      |      "client": {"organisationId": 123, "organisationName": "ABC"}
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW99",
      |      "checkCaseReference": "CHK100028784",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-31T07:21:11",
      |      "submittedBy": "OA Ltd",
      |      "client": {"organisationId": 123, "organisationName": "ABC"}
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW5B",
      |      "checkCaseReference": "CHK100028650",
      |      "checkCaseStatus": "PENDING",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-18T08:06:51",
      |      "submittedBy": "OA Ltd",
      |      "client": {"organisationId": 123, "organisationName": "ABC"}
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW39",
      |      "checkCaseReference": "CHK100028589",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-10T10:10:11",
      |      "submittedBy": "OA Ltd",
      |      "client": {"organisationId": 123, "organisationName": "ABC"}
      |    }
      |  ]
      |}
    """.stripMargin

  val ownerAgents =
    """
      |{
      |  "start": 1,
      |  "size": 15,
      |  "filterTotal": 4,
      |  "total": 4,
      |  "checkCases": [
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW9Q",
      |      "checkCaseReference": "CHK100028783",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-31T08:16:54",
      |      "submittedBy": "OA Ltd"
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW99",
      |      "checkCaseReference": "CHK100028784",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-31T07:21:11",
      |      "submittedBy": "OA Ltd"
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW5B",
      |      "checkCaseReference": "CHK100028650",
      |      "checkCaseStatus": "PENDING",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-18T08:06:51",
      |      "submittedBy": "OA Ltd"
      |    },
      |    {
      |      "checkCaseSubmissionId": "CHK-1ZRPW39",
      |      "checkCaseReference": "CHK100028589",
      |      "checkCaseStatus": "OPEN",
      |      "address": "Not Available",
      |      "uarn": 7538840000,
      |      "createdDateTime": "2018-07-10T10:10:11",
      |      "submittedBy": "OA Ltd"
      |    }
      |  ]
      |}
    """.stripMargin

}
