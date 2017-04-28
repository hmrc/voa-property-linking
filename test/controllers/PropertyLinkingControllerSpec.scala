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
