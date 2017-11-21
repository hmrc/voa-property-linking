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

package connectors

import java.time.{Clock, Instant, ZoneId}

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import models.{GroupAccountSubmission, IndividualAccountSubmissionForOrganisation, IndividualDetails}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

class GroupAccountConnectorSpec extends WireMockSpec with SimpleWsHttpTestApplication {

  "Creating an organisation account" should {
    "serialise to schema-valid JSON" in {
      val testSubmission = GroupAccountSubmission(
        id = "groupId",
        companyName = "a company",
        addressId = 1,
        email = "aa@bb.cc",
        phone = "123",
        isAgent = false,
        individualAccountSubmission = IndividualAccountSubmissionForOrganisation(
          externalId = "externalId",
          trustId = "trustId",
          details = IndividualDetails(
            firstName = "firstName",
            lastName = "lastName",
            email = "aa@bb.cc",
            phone1 = "123",
            phone2 = None,
            addressId = 2
          )
        )
      )

      stubFor(post(urlEqualTo("/customer-management-api/organisation")).withRequestBody(equalToJson(Json.stringify(expectedRequest)))
          .willReturn(aResponse.withBody(Json.stringify(expectedResponse)))
      )

      Json.toJson(await(testConnector.create(testSubmission)(HeaderCarrier()))) shouldBe expectedResponse
    }
  }

  lazy val testConnector = new GroupAccountConnector(new SimpleWSHttp, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl = mockServerUrl
  }

  lazy val expectedRequest = Json.obj(
    "governmentGatewayGroupId" -> "groupId",
    "addressUnitId" -> 1,
    "representativeFlag" -> false,
    "organisationName" -> "a company",
    "organisationEmailAddress" -> "aa@bb.cc",
    "organisationTelephoneNumber" -> "123",
    "effectiveFrom" -> Instant.now(fixedClock).toString,
    "personData" -> Json.obj(
      "governmentGatewayExternalId" -> "externalId",
      "addressUnitId" -> 2,
      "firstName" -> "firstName",
      "lastName" -> "lastName",
      "emailAddress" -> "aa@bb.cc",
      "telephoneNumber" -> "123",
      "identifyVerificationId" -> "trustId",
      "effectiveFrom" -> Instant.now(fixedClock).toString
    )
  )

  lazy val expectedResponse = Json.obj(
    "id" -> 1,
    "message" -> "Something",
    "responseTime" -> 99999999
  )

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())
}
