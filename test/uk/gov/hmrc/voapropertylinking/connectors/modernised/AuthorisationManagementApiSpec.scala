/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.Future

class AuthorisationManagementApiSpec extends BaseUnitSpec {

  val http: DefaultHttpClient = mock[DefaultHttpClient]
  val connector: AuthorisationManagementApi = new AuthorisationManagementApi(
    http,
    mockServicesConfig,
    "http://localhost:9540/authorisation-management-api/agent/submit_agent_representation",
    "http://localhost:9540authorisation-management-api/agent/submit_agent_rep_reponse"
  ) {
    override lazy val baseUrl: String = "http://some-url"
  }

  "AuthorisationManagementApi.validateAgentCode" should {
    "return the organisation id for a valid agentCode" in {
      val agentCode = 123
      val authId = 987

      when(http.GET[JsValue](any())(any(), any(), any()))
        .thenReturn(Future.successful(Json.parse(validAgentCodeResponse)))

      inside(connector.validateAgentCode(agentCode, authId)(hc).futureValue) {
        case Left(v) => v shouldBe 1234567
      }
    }
  }

  "AuthorisationManagementApi.validateAgentCode" should {
    "return the invalid code for a valid agentCode" in {

      val agentCode = 123
      val authId = 987

      when(http.GET[JsValue](any())(any(), any(), any()))
        .thenReturn(Future.successful(Json.parse(noAgentFlagResponse)))

      inside(connector.validateAgentCode(agentCode, authId)(hc).futureValue) {
        case Right(v) => v shouldBe "INVALID_CODE"
      }
    }
  }

  "AuthorisationManagementApi.validateAgentCode" should {
    "return the invalid code which is not a no Agent Flag code" in {

      val agentCode = 123
      val authId = 987

      when(http.GET[JsValue](any())(any(), any(), any()))
        .thenReturn(Future.successful(Json.parse(invalidAgentCodeResponse)))

      inside(connector.validateAgentCode(agentCode, authId)(hc).futureValue) {
        case Right(v) => v shouldBe "OTHER_ERROR"
      }
    }
  }

  "AuthorisationManagementApi.response" should {
    "return a unit after putting a property representation response" in {
      val response = APIRepresentationResponse(
        submissionId = "abc123",
        authorisedPartyPersonId = 24680,
        outcome = "ok"
      )

      when(http.PUT[APIRepresentationResponse, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      val result: Unit = connector.response(response)(hc).futureValue
      result shouldBe (())
    }
  }

  private lazy val validAgentCodeResponse =
    """{
      |  "isValid": true,
      |  "organisationId": 1234567,
      |  "failureCode": "NO_AGENT_FLAG"
      |}""".stripMargin

  private lazy val noAgentFlagResponse =
    """{
      |  "isValid": false,
      |  "organisationId": 1234567,
      |  "failureCode": "NO_AGENT_FLAG"
      |}""".stripMargin

  private lazy val invalidAgentCodeResponse =
    """{
      |  "isValid": false,
      |  "organisationId": 1234567,
      |  "failureCode": "OTHER_ERROR"
      |}""".stripMargin

}
