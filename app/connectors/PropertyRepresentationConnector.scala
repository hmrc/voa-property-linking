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

import javax.inject.{Inject, Named}

import models._
import org.joda.time.LocalDate
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector @Inject()(@Named("VoaBackendWsHttp") http: WSHttp)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")

  def validateAgentCode(agentCode: Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[Either[Long, String]] = {
    val url = baseUrl + s"/authorisation-management-api/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authorisationId"
    http.GET[JsValue](url).map(js => {
      val valid = (js \ "isValid").as[Boolean]
      if (valid)
        Left((js \ "organisationId").as[Long])
      else {
        val code = (js \ "failureCode").as[String]
        Right(
          code match {
            case "NO_AGENT_FLAG" => "INVALID_CODE"
            case _ => code
          })
      }
    })
  }

  def forAgent(status: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    val params = s"status=$status&organisationId=$organisationId&startPoint=1"
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/agent_representation_requests?$params"
    http.GET[APIPropertyRepresentations](url).map(x => {
      PropertyRepresentations(
        x.totalPendingRequests,
        x.requests
          //representationId is actually the AgentOrgId, and the backend currently returns
          //representation requests that don't belong to the orgId we are interested in.
          .filter(_.representationId == organisationId)
          .map(_.toPropertyRepresentation))
    })
  }

  def create(reprRequest: APIRepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/authorisation-management-api/agent/submit_agent_representation"
    http.POST[APIRepresentationRequest, HttpResponse](url, reprRequest) map { _ => () }
  }

  def response(representationResponse: APIRepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/authorisation-management-api/agent/submit_agent_rep_reponse"
    http.PUT[APIRepresentationResponse, HttpResponse](url, representationResponse) map { _ => () }
  }

  def revoke(authorisedPartyId: Long)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/authorisation-management-api/authorisedParty/$authorisedPartyId"
    http.PATCH[JsValue, HttpResponse](url,
      Json.obj("endDate" -> s"${LocalDate.now.toString}",
        "authorisedPartyStatus" -> "REVOKED")) map { _ => () }
  }

}
