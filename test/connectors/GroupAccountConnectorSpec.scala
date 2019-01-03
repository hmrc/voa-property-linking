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

import java.time._

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class GroupAccountConnectorSpec extends ContentTypes with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val testConnector = new GroupAccountConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl = mockServerUrl
  }
  val url = s"/customer-management-api/organisation"

  "GroupAccountConnector.get" should {
    "return the group accounts associated with the provided id" in {
      val groupId = 1234L
      val getUrl = s"$url?organisationId=$groupId"
      val stub = stubFor(get(urlEqualTo(getUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.get(groupId)(hc))) shouldBe expectedGetValidResponse1
    }

      "return an empty response if the provided id cannot be found" in {
        val groupId = 1234L
        val getUrl = s"$url?organisationId=$groupId"
        val stub = stubFor(get(urlEqualTo(getUrl))
          .willReturn(aResponse
            .withStatus(404)
            .withHeader("Content-Type", JSON)
            .withBody(getResponseNotFound)))

        val result = (await(testConnector.get(groupId)(hc))) shouldBe expectedGetEmptyResponse
      }
    }

  "GroupAccountConnector.findByGGID" should {
    "return the group accounts associated with the provided GGID" in {
      val ggId = "1234"
      val findByGGIDUrl = s"$url?governmentGatewayGroupId=$ggId"
      val stub = stubFor(get(urlEqualTo(findByGGIDUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.findByGGID(ggId)(hc))) shouldBe expectedGetValidResponse1
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"
      val findByGGIDUrl = s"$url?governmentGatewayGroupId=$ggId"
      val stub = stubFor(get(urlEqualTo(findByGGIDUrl))
        .willReturn(aResponse
          .withStatus(404)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseNotFound)))

      val result = (await(testConnector.findByGGID(ggId)(hc))) shouldBe expectedGetEmptyResponse
    }
  }

  "GroupAccountConnector.withAgentCode" should {
    "return the group accounts associated with the provided agent code" in {
      val agentCode = "ac234"
      val withAgentCodeUrl = s"$url?representativeCode=$agentCode"
      val stub = stubFor(get(urlEqualTo(withAgentCodeUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.withAgentCode(agentCode)(hc))) shouldBe expectedGetValidResponse1
    }


    "return an empty response if the provided agent code cannot be found" in {
      val agentCode = "ac234"
      val withAgentCodeUrl = s"$url?representativeCode=$agentCode"
      val stub = stubFor(get(urlEqualTo(withAgentCodeUrl))
        .willReturn(aResponse
          .withStatus(404)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseNotFound)))

      val result = (await(testConnector.withAgentCode(agentCode)(hc))) shouldBe expectedGetEmptyResponse
    }
  }

  "GroupAccountConnector.create" should {
    "return the created account's group id" in {

      val stub = stubFor(post(urlEqualTo(url))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(createResponseValid)))

      val result = (await(testConnector.create(createValidRequest)(hc))) shouldBe expectedCreateValidResponse
    }
  }

  "GroupAccountConnector.update" should {
    "return unit after updating the account" in {

      val orgId = 123456789
      val updatedOrgAccount = UpdatedOrganisationAccount(
        governmentGatewayGroupId = "gggId1",
        addressUnitId = 1234567,
        representativeFlag = true,
        organisationName = "Fake News Inc",
        organisationEmailAddress = "therealdonald@whitehouse.com",
        organisationTelephoneNumber = "0987612345",
        effectiveFrom = instant,
        changedByGGExternalId = "tester1"
      )

      val stub = stubFor(put(urlEqualTo(s"$url/$orgId"))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(createResponseValid)))

      val result = (await(testConnector.update(orgId = orgId, updatedOrgAccount)))
      result should be ()
    }
  }

  lazy val createValidRequest: GroupAccountSubmission = GroupAccountSubmission(
    id = "acc123",
    companyName = "Real news Inc",
    addressId = 9876543L,
    email = "thewhitehouse@potus.com",
    phone = "01987654",
    isAgent = false,
    individualAccountSubmission = IndividualAccountSubmissionForOrganisation(
      externalId = "Ext123",
      trustId= "trust234",
      details = IndividualDetails(
        firstName = "Donald",
        lastName = "Trump",
        email = "therealdonald@potus.com",
        phone1= "123456789",
        phone2= Some("987654321"),
        addressId= 24680L
      )
    )
  )

    lazy val getResponseValid = """{
    "id": 2,
    "governmentGatewayGroupId": "gggId",
    "representativeCode":234,
    "organisationLatestDetail": {
      "addressUnitId": 345,
      "representativeFlag": false,
      "organisationName": "Fake News Inc",
      "organisationEmailAddress": "therealdonald@potus.com",
      "organisationTelephoneNumber": "9876541"
      },
      "persons": [
        {
          "personLatestDetail": {
            "addressUnitId": 9876,
            "firstName": "anotherFirstName",
            "lastName": "anotherLastName",
            "emailAddress": "theFakeDonald@potus.com",
            "telephoneNumber": "24680",
            "mobileNumber": "13579",
           "identifyVerificationId": "idv1"
          }
        }
      ]
  }"""

  lazy val createResponseValid =
    """{
      |"id": 654321,
      |"message": "valid group id",
      |"responseTime": 45678
      |}""".stripMargin

  lazy val expectedCreateValidResponse = GroupId(
    id = 654321L,
    message="valid group id",
    responseTime = 45678)

  lazy val expectedGetValidResponse1 = Some(GroupAccount(
    id = 2,
    groupId = "gggId",
    companyName = "Fake News Inc",
    addressId=345,
    email="therealdonald@potus.com",
    phone = "9876541",
    isAgent = false,
    agentCode = 234)
  )

  lazy val getResponseNotFound = "{}"

  lazy val expectedGetEmptyResponse = None

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  val date = LocalDate.parse("2018-09-05")
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
}
