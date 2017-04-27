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

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors._
import helpers.WithSimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import models._
import org.joda.time.{DateTime, LocalDate}
import play.api.http.ContentTypes
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class PropertyLinkingControllerSpec
  extends WireMockSpec with WithSimpleWsHttpTestApplication with ContentTypes {

  implicit val request = FakeRequest()

  var testPropertyLinkingController: PropertyLinkingController = _

  override def beforeAll() {
    super.beforeAll

    val http = fakeApplication.injector.instanceOf[SimpleWSHttp]

    val propertyLinksConnector = new PropertyLinkingConnector(http) {
      override lazy val baseUrl: String = mockServerUrl
    }

    val addressesConnector = new AddressConnector(http)

    val groupAccountsConnector: GroupAccountConnector = new GroupAccountConnector(addressesConnector, http) {
      override lazy val baseUrl: String = mockServerUrl
    }

    val representationsConnector = new PropertyRepresentationConnector(http) {
      override lazy val baseUrl: String = mockServerUrl
    }

    testPropertyLinkingController = new PropertyLinkingController(propertyLinksConnector, groupAccountsConnector, representationsConnector)
  }

  "clientProperties" should {
    "only show the properties and permissions for that agent" in {
      val userOrgId = 111
      val agentOrgId = 222
      val otherAgentOrgId = 333

      val dummyProperties = Seq(
        //prop with noAgents
        PropertiesView(100, 1, userOrgId, 5, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Nil),
        //prop with agent
        PropertiesView(100, 2, userOrgId, 6, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(APIParty(1, "APPROVED", agentOrgId, Seq(Permissions(1, "CONTINUE_ONLY", "CONTINUE_ONLY", None))))),
        //prop with OtherAgent
        PropertiesView(100, 3, userOrgId, 7, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(APIParty(2, "APPROVED", otherAgentOrgId, Seq(Permissions(2, "CONTINUE_ONLY", "CONTINUE_ONLY", None))))),
        //prop with agent and OtherAgent
        PropertiesView(100, 4, userOrgId, 8, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Seq(
            APIParty(3, "APPROVED", otherAgentOrgId, Seq(Permissions(3, "START_AND_CONTINUE", "CONTINUE_ONLY", None))),
            APIParty(4, "APPROVED", agentOrgId, Seq(Permissions(4, "CONTINUE_ONLY", "NOT_PERMITTED", None)))
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


      val propertiesUrl = s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
        s"?listYear=2017" +
        s"&organisationId=$userOrgId" +
        s"&startPoint=1" +
        s"&pageSize=25" +
        s"&requestTotalRowCount=false"

      stubFor(get(urlEqualTo(propertiesUrl))
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

      stubFor(get(urlEqualTo(s"/customer-management-api/organisation?organisationId=${otherAgentOrgId}"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(Json.toJson(dummyAgentGroupAccount).toString)
        )
      )

      val res = testPropertyLinkingController.clientProperties(userOrgId, agentOrgId, PaginationParams(1, 25, requestTotalRowCount = false))(FakeRequest())
      status(res) shouldBe OK
      val uarnsAndPermIds = contentAsJson(res).as[ClientPropertyResponse].properties.map(x => (x.uarn, x.permissionId))
      uarnsAndPermIds shouldBe Seq((2, 1), (4, 4))
    }
  }

  "find" should {
    "only return the user's properties" in {
      val userOrgId = 111

      val dummyProperties = Seq(
        PropertiesView(101, 101, userOrgId, 103, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Nil),
        PropertiesView(102, 102, userOrgId, 104, "AAA", "ASDf", "string", DateTime.now(), LocalDate.now(), None, "1231", Nil,
          Nil)
      )

      val propertiesUrl = s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
        s"?listYear=2017" +
        s"&organisationId=$userOrgId" +
        s"&startPoint=1" +
        s"&pageSize=25" +
        s"&requestTotalRowCount=false"

      stubFor(get(urlEqualTo(propertiesUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(s"""{"authorisations": ${Json.toJson(dummyProperties).toString}}""")
        )
      )

      stubFor(get(urlEqualTo(s"/mdtp-dashboard-management-api/mdtp_dashboard/agent_representation_requests?status=APPROVED&organisationId=$userOrgId&startPoint=1"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(Json.toJson(APIPropertyRepresentations(0, Some(0), Nil)).toString)
        )
      )

      val res = testPropertyLinkingController.find(userOrgId, PaginationParams(1, 25, requestTotalRowCount = false))(FakeRequest())
      status(res) shouldBe OK
      val uarns = Json.parse(contentAsString(res)).as[PropertyLinkResponse].propertyLinks.map(_.uarn)
      uarns shouldBe Seq(101, 102)
    }
  }
}
