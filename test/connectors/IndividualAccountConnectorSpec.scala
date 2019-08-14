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

import basespecs.WireMockSpec
import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

class IndividualAccountConnectorSpec extends WireMockSpec with ContentTypes with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val config = fakeApplication.injector.instanceOf[ServicesConfig]
  val testConnector = new IndividualAccountConnector(fakeApplication.injector.instanceOf[AddressConnector], http, config) {
    override lazy val baseUrl = mockServerUrl
  }

  val url = s"/customer-management-api/person"

  "IndividualAccountConnector.get" should {
    "return the individual account associated with the provided person id" in {
      val id = 1234L
      val getUrl = s"$url?personId=$id"
      val stub = stubFor(get(urlEqualTo(getUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.get(id)(hc))) shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided id cannot be found" in {
      val id = 1234L
      val getUrl = s"$url?personId=1234"
      val stub = stubFor(get(urlEqualTo(getUrl))
        .willReturn(aResponse
          .withStatus(404)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseNotFound)))

      val result = (await(testConnector.get(id)(hc))) shouldBe expectedGetEmptyResponse
    }
  }

  "IndividualAccountConnector.findByGGID" should {
    "return the individual account associated with the provided GGID" in {
      val ggId = "1234"
      val findByGGIDUrl = s"$url?governmentGatewayExternalId=$ggId"
      val stub = stubFor(get(urlEqualTo(findByGGIDUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.findByGGID(ggId)(hc))) shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"
      val findByGGIDUrl = s"$url?governmentGatewayExternalId=$ggId"
      val stub = stubFor(get(urlEqualTo(findByGGIDUrl))
        .willReturn(aResponse
          .withStatus(404)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseNotFound)))

      val result = (await(testConnector.findByGGID(ggId)(hc))) shouldBe expectedGetEmptyResponse
    }
  }

  "IndividualAccountConnector.create" should {
    "return an individual account id for the individual account submission" in {
      val personId = 1234L
      val createUrl = s"$url"
      val stub = stubFor(post(urlEqualTo(createUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(createResponseValid)))

      val result = (await(testConnector.create(individualAccountSubmission)(hc))) shouldBe expectedCreateResponseValid
    }
  }

  "IndividualAccountConnector.update" should {
    "update the person id with the individual account submission" in {
      val personId = 1234L
      val updateUrl = s"$url/$personId"
      val stub = stubFor(put(urlEqualTo(updateUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getResponseValid)))

      val result = (await(testConnector.update(personId, individualAccountSubmission)(hc))) shouldBe expectedUpdateValidResponse
    }
  }

  lazy val expectedCreateResponseValid =  IndividualAccountId(12345)

  lazy val individualAccountSubmission = IndividualAccountSubmission(
    externalId = "ggEId12",
    trustId = "idv1",
    organisationId = 13579,
    details = IndividualDetails(
      firstName  = "Kim",
      lastName = "Yong Un",
      email = "thechosenone@nkorea.nk",
      phone1 = "24680",
      phone2 = Some("13579"),
      addressId = 9876
    )
  )

  lazy val createResponseValid =
    """{
      |"id": 12345
      |}""".stripMargin

  lazy val getResponseValid = """{
    "id": 2,
    "governmentGatewayExternalId": "ggEId12",
    "personLatestDetail": {
      |"addressUnitId": 9876,
      |"firstName": "anotherFirstName",
      |"lastName": "anotherLastName",
      |"emailAddress": "theFakeDonald@potus.com",
      |"telephoneNumber": "24680",
      |"mobileNumber": "13579",
      |"identifyVerificationId": "idv1"
    },
    "organisationId": 13579,
    "organisationLatestDetail": {
      "addressUnitId": 345,
      "representativeFlag": false,
      "organisationName": "Fake News Inc",
      "organisationEmailAddress": "therealdonald@potus.com",
      "organisationTelephoneNumber": "9876541"
      }
  }""".stripMargin

  lazy val expectedGetValidResponse = Some(IndividualAccount(
    externalId = "ggEId12",
    trustId = "idv1",
    organisationId = 13579,
    individualId = 2,
    details = IndividualDetails(
      firstName="anotherFirstName",
      lastName="anotherLastName",
      email= "theFakeDonald@potus.com",
      phone1= "24680",
      phone2= Some("13579"),
      addressId= 9876
      )
    )
  )

  val expectedUpdateValidResponse = Json.parse("""{
    "id": 2,
    "governmentGatewayExternalId": "ggEId12",
    "personLatestDetail": {
      |"addressUnitId": 9876,
      |"firstName": "anotherFirstName",
      |"lastName": "anotherLastName",
      |"emailAddress": "theFakeDonald@potus.com",
      |"telephoneNumber": "24680",
      |"mobileNumber": "13579",
      |"identifyVerificationId": "idv1"
    },
    "organisationId": 13579,
    "organisationLatestDetail": {
      "addressUnitId": 345,
      "representativeFlag": false,
      "organisationName": "Fake News Inc",
      "organisationEmailAddress": "therealdonald@potus.com",
      "organisationTelephoneNumber": "9876541"
      }
  }""".stripMargin)


  lazy val expectedGetEmptyResponse = None
  lazy val getResponseNotFound = "{}"

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
}
