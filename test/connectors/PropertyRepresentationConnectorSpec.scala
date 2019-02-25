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

import java.time.{LocalDate, ZoneOffset}

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models._
import play.api.http.ContentTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class PropertyRepresentationConnectorSpec extends ContentTypes
    with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val config = fakeApplication.injector.instanceOf[ServicesConfig]
  val connector = new PropertyRepresentationConnector(http, config) {
    override lazy val baseUrl: String = mockServerUrl
  }

  "PropertyRepresentationConnector.validateAgentCode" should {
    "return the organisation id for a valid agentCode" in {

      val agentCode = 123
      val authId = 987
      val agentUrl = s"/authorisation-management-api/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authId"

      stubFor(get(urlEqualTo(agentUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validAgentCodeResponse)
        )
      )
      val result: Either[Long, String] = await(connector.validateAgentCode(agentCode, authId)(hc))
      result match {
        case Left(v) => v shouldBe(1234567)
      }
    }
  }

  "PropertyRepresentationConnector.validateAgentCode" should {
    "return the invalid code for a valid agentCode" in {

      val agentCode = 123
      val authId = 987
      val agentUrl = s"/authorisation-management-api/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authId"

      stubFor(get(urlEqualTo(agentUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(noAgentFlagResponse)
        )
      )
      val result: Either[Long, String] = await(connector.validateAgentCode(agentCode, authId)(hc))
      result match {
        case Right(v) => v shouldBe("INVALID_CODE")
      }
    }
  }

  "PropertyRepresentationConnector.validateAgentCode" should {
    "return the invalid code which is not a no Agent Flag code" in {

      val agentCode = 123
      val authId = 987
      val agentUrl = s"/authorisation-management-api/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authId"

      stubFor(get(urlEqualTo(agentUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(invalidAgentCodeResponse)
        )
      )
      val result: Either[Long, String] = await(connector.validateAgentCode(agentCode, authId)(hc))
      result match {
        case Right(v) => v shouldBe("OTHER_ERROR")
      }
    }
  }

  "PropertyRepresentationConnector.forAgent" should {
    "return the invalid code which is not a no Agent Flag code" in {

      val pageParams = PaginationParams(0, 10, false)
      val organisationId = 98765
      val authorisationUrl = s"/authorisation-search-api/agents/$organisationId/authorisations" +
        s"?start=${pageParams.startPoint}" +
        s"&size=${pageParams.pageSize}" +
        s"&representationStatus=PENDING"

      stubFor(get(urlEqualTo(authorisationUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validAgentAuthResultBE)
        )
      )

      val validPropertyRepresentation = PropertyRepresentation(
        authorisationId = 987654,
        billingAuthorityReference = "VOA1",
        submissionId = "xyz123",
        organisationId = 123456,
        organisationName = "Fake News Inc",
        address = "The White House",
        checkPermission = "valid",
        challengePermission = "invalid",
        createDatetime = LocalDate.now(),
        status = "ok"
      )

      val validPropertyRepresentations = PropertyRepresentations(
        totalPendingRequests = 30,
        propertyRepresentations = Seq(validPropertyRepresentation)
      )

      val result: PropertyRepresentations = await(connector.forAgent(status = "ok", organisationId, pageParams)(hc))
      result shouldBe validPropertyRepresentations
    }
  }

  "PropertyRepresentationConnector.create" should {
    "return a created property representation" in {

      val createUrl = s"/authorisation-management-api/agent/submit_agent_representation"

      val createRequest = APIRepresentationRequest(
        authorisationId = 123456,
        submissionId = "abc123",
        authorisationOwnerPersonId = 98765,
        authorisedPartyOrganisationId = 24680,
        checkPermission = "ok",
        challengePermission = "notOk",
        createDatetime = instant
      )

      stubFor(post(urlEqualTo(createUrl))
         .willReturn(aResponse
           .withStatus(200)
           .withHeader("Content-Type", JSON)
           .withBody(validEmptyResponse)
         )
      )

      val result: Unit = await(connector.create(createRequest)(hc))
      result shouldBe ()
    }
  }

  "PropertyRepresentationConnector.response" should {
    "return a unit after putting a property representation response" in {

      val responseUrl = s"/authorisation-management-api/agent/submit_agent_rep_reponse"
      val response = APIRepresentationResponse(
        submissionId = "abc123",
        authorisedPartyPersonId = 24680,
        outcome = "ok"
      )

      stubFor(put(urlEqualTo(responseUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validEmptyResponse)
        )
      )

      val result: Unit = await(connector.response(response)(hc))
      result shouldBe ()
    }
  }

  "PropertyRepresentationConnector.revoke" should {
    "return a unit after revoking property representation response" in {

      val authorisedPartyId =  34567890
      val revokeUrl = s"/authorisation-management-api/authorisedParty/$authorisedPartyId"

      stubFor(patch(urlEqualTo(revokeUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validRevokeResponse)
        )
      )
      val result: Unit = await(connector.revoke(authorisedPartyId)(hc))
      result shouldBe ()
    }
  }

  lazy val validEmptyResponse = "{}"

  lazy val validRevokeResponse =
    s"""{
      | "endDate": ${date.toString},
      | "authorisedPartyStatus": "REVOKED"
      |}""".stripMargin

  lazy val validAgentCodeResponse =
    """{
      |  "isValid": true,
      |  "organisationId": 1234567,
      |  "failureCode": "NO_AGENT_FLAG"
      |}""".stripMargin

  lazy val noAgentFlagResponse =
    """{
      |  "isValid": false,
      |  "organisationId": 1234567,
      |  "failureCode": "NO_AGENT_FLAG"
      |}""".stripMargin

  lazy val invalidAgentCodeResponse =
    """{
      |  "isValid": false,
      |  "organisationId": 1234567,
      |  "failureCode": "OTHER_ERROR"
      |}""".stripMargin

  lazy val validPropertyRepresentationsResponse =
    """{
      | "totalPendingRequests": 1,
      | "propertyRepresentations": [
        | {
        |  "authorisationId": 987654,
        |  "billingAuthorityReference": "VOA1",
        |  "submissionId": "xyz123",
        |  "organisationId": 123456,
        |  "organisationName": "Fake News Inc",
        |  "address": "The White House",
        |  "checkPermission": "valid",
        |  "challengePermission": "invalid",
        |  "createDatetime": null,
        |  "status": "ok"
        | }
      |]
      |}""".stripMargin


  lazy val validAgentAuthResultBE =
    """{
      |"start": 0,
      |"size": 10,
      |"filterTotal": 30,
      |"total": 50,
      |"authorisations": [
       | {
       |  "authorisationId": 987654,
       |  "authorisedPartyId": 34567890,
       |  "status": "ok",
       |  "representationSubmissionId": "xyz123",
       |  "submissionId": "123xyz",
       |  "uarn": 123456,
       |  "address": "The White House",
       |  "localAuthorityRef": "VOA1",
       |  "client": {
       |    "organisationId": 123456,
       |    "organisationName": "Fake News Inc"
      |   },
       |  "representationStatus":"NotOK",
       |  "checkPermission": "valid",
       |  "challengePermission": "invalid"
       | }
     |]
    }""".stripMargin

    val date = LocalDate.parse("2018-09-04")
    val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
}
