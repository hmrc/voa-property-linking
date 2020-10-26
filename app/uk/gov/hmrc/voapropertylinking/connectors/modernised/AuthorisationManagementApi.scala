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

import javax.inject.{Inject, Named}
import models.APIRepresentationResponse
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationManagementApi @Inject()(
      http: HttpClient,
      servicesConfig: ServicesConfig,
      @Named("voa.representationRequestResponse") representationRequestResponseUrl: String
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val baseUrl
    : String = servicesConfig.baseUrl("external-business-rates-data-platform") + "/authorisation-management-api"

  def validateAgentCode(agentCode: Long, authorisationId: Long)(
        implicit hc: HeaderCarrier): Future[Either[Long, String]] = {
    val url = baseUrl + s"/agent/validate_agent_code?agentCode=$agentCode&authorisationId=$authorisationId"
    http
      .GET[JsValue](url)
      .map(js => {
        val valid = (js \ "isValid").as[Boolean]
        if (valid)
          Left((js \ "organisationId").as[Long])
        else {
          val code = (js \ "failureCode").as[String]
          Right(code match {
            case "NO_AGENT_FLAG" => "INVALID_CODE"
            case _               => code
          })
        }
      })
  }

  def response(representationResponse: APIRepresentationResponse)(implicit hc: HeaderCarrier): Future[HttpResponse] =
    http
      .PUT[APIRepresentationResponse, HttpResponse](representationRequestResponseUrl, representationResponse)

}
