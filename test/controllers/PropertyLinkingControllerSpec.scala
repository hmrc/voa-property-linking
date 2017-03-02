/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.Wiring
import connectors.{GroupAccountConnector, PropertyLinkingConnector, WireMockSpec}
import com.github.tomakehurst.wiremock.client.WireMock._
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._


class PropertyLinkingControllerSpec //extends ControllerSpec
  extends ContentTypes
  with WireMockSpec{

  implicit val request = FakeRequest()

  object TestPropertyLinkingController extends PropertyLinkingController {
    override val propertyLinksConnector = new PropertyLinkingConnector(Wiring().http) {
      override lazy val baseUrl: String = mockServerUrl
    }
    override val groupAccountsConnector: GroupAccountConnector = new GroupAccountConnector(Wiring().http) {
      override lazy val baseUrl: String = mockServerUrl
    }
  }

  "clientProperties" should {
    "only show the properties assigned to an agent" in {
      val userOrgId = 111
      val agentOrgId = 222
      val otherAgentOrgId = 333

      val dummyProperties = Seq (
        //prop with noAgents
        APIAuthorisation(100, 1, userOrgId, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Nil),
        //prop with agent
        APIAuthorisation(100, 2, userOrgId, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(APIParty(agentOrgId, Seq(Permissions("CONTINUE_ONLY", "CONTINUE_ONLY"))))),
        //prop with OtherAgent
        APIAuthorisation(100, 3, userOrgId, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(APIParty(otherAgentOrgId, Seq(Permissions("CONTINUE_ONLY", "CONTINUE_ONLY"))))),
        //prop with agent and OtherAgent
        APIAuthorisation(100, 4, userOrgId, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(
            APIParty(otherAgentOrgId, Seq(Permissions("CONTINUE_ONLY", "CONTINUE_ONLY"))),
            APIParty(agentOrgId, Seq(Permissions("CONTINUE_ONLY", "CONTINUE_ONLY")))
          )
        )
      )
      val dummyUserGroupAccount = APIDetailedGroupAccount(
        userOrgId, "123", 1234, GroupDetails(1, true, true, "UserCompany", "aaa@aaa.com", None, LocalDate.now()), Nil
      )
      val dummyAgentGroupAccount = APIDetailedGroupAccount(
        agentOrgId, "123", 1234, GroupDetails(1, true, true, "UserCompany", "aaa@aaa.com", None, LocalDate.now()), Nil
      )
      val dummyOtherAgentGroupAccount = APIDetailedGroupAccount(
        otherAgentOrgId, "123", 1234, GroupDetails(1, true, true, "UserCompany", "aaa@aaa.com", None, LocalDate.now()), Nil
      )

      stubFor(get(urlEqualTo(s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view?listYear=2017&organisationId=${userOrgId}"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(s"""{"authorisations": ${Json.toJson(dummyProperties).toString}}""")
        )
      )
      stubFor(get(urlEqualTo(s"/customer-management-api/organisation?organisationId=${userOrgId}"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(Json.toJson(dummyUserGroupAccount).toString)
        )
      )
      stubFor(get(urlEqualTo(s"/customer-management-api/organisation?organisationId=${agentOrgId}"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(Json.toJson(dummyAgentGroupAccount).toString)
        )
      )
      val res = TestPropertyLinkingController.clientProperties(userOrgId, agentOrgId)(FakeRequest())
      status(res) shouldBe OK
      val uarns = Json.parse(contentAsString(res)).as[Seq[ClientProperties]].map(_.uarn)
       uarns shouldBe Seq(2, 4)
      println(uarns)
    }
  }


}
