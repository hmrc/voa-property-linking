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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import java.time.LocalDate

import javax.inject.Inject
import models.{APIRepresentationRequest, APIRepresentationResponse, Capacity}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationManagementApi @Inject()(
                                          http: DefaultHttpClient,
                                          servicesConfig: ServicesConfig
                                          )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform") + "/authorisation-management-api"

  def validateAgentCode(agentCode: Long, authorisationId: Long)(implicit hc: HeaderCarrier): Future[Either[Long, String]] = {
    val url = baseUrl + s"/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authorisationId"
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

  def getCapacity(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[Capacity]] = {
    val url = baseUrl + s"/authorisation/$authorisationId"

    http.GET[Option[Capacity]](url)
  }

  def create(reprRequest: APIRepresentationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/agent/submit_agent_representation"
    http.POST[APIRepresentationRequest, HttpResponse](url, reprRequest) map { _ => () }
  }

  def response(representationResponse: APIRepresentationResponse)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/agent/submit_agent_rep_reponse"
    http.PUT[APIRepresentationResponse, HttpResponse](url, representationResponse) map { _ => () }
  }

  def revoke(authorisedPartyId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = baseUrl + s"/authorisedParty/$authorisedPartyId"
    http.PATCH[JsValue, HttpResponse](url,
      Json.obj(
        "endDate" -> LocalDate.now.toString,
        "authorisedPartyStatus" -> "REVOKED"
      ))
  }
}