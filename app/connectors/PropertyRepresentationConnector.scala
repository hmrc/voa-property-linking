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

import config.VOABackendWSHttp.InvalidAgentCode
import models._
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")

  def validateAgentCode(agentCode:Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[Either[Long, String]] = {
    val url = baseUrl  + s"/authorisation-management-api/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authorisationId"
    http.GET[JsValue](url).map( js => {
      Left((js \ "organisationId").as[Long])
    }) recover {
      case b: InvalidAgentCode => {
        val code = (b.body \ "failureCode").as[String]
        Right(
          code match {
            case "NO_AGENT_FLAG" => "INVALID_CODE"
            case _ => code
          })
      }
    }
  }

  def get(representationId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyRepresentation]] = {
    val url = baseUrl  + "/property-representations"+ s"/$representationId"
    http.GET[Option[PropertyRepresentation]](url)
  }

  def find(authorisationId: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyRepresentation]] = {
    val url = baseUrl  + "/property-representations"+ s"/find/$authorisationId"
    http.GET[Seq[PropertyRepresentation]](url)
  }

  def forAgent(status: String, organisationId: Long)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    val params=s"status=$status&organisationId=$organisationId&startPoint=1"
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/agent_representation_requests?$params"
    http.GET[APIPropertyRepresentations](url).map( x => {
      PropertyRepresentations(
        x.totalPendingRequests,
        x.requests.map(_.toPropertyRepresentation))
    })
  }

  def create(reprRequest: APIRepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl  + s"/authorisation-management-api/agent/submit_agent_representation"
    http.POST[APIRepresentationRequest, HttpResponse](url, reprRequest) map { _ => () }
  }

  def response(representationResponse: APIRepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl  + s"/authorisation-management-api/agent/submit_agent_rep_reponse"
    http.PUT[APIRepresentationResponse, HttpResponse](url, representationResponse) map { _ => () }
  }

  def update(reprRequest: UpdatedRepresentation)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl  + "/property-representations"+ s"/update"
    http.PUT[UpdatedRepresentation, HttpResponse](url, reprRequest) map { _ => () }
  }

}
