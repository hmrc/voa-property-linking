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

import http.VoaHttpClient
import javax.inject.{Inject, Named}
import models.voa.valuation.dvr.DetailedValuationRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

class DVRCaseManagementConnector @Inject()(
                                            wsClient: WSClient,
                                            @Named("VoaBackendWsHttp") ws: WSHttp,
                                            @Named("VoaAuthedBackendHttp") http: VoaHttpClient,
                                            config: ServicesConfig
                                          ) extends HttpErrorFunctions {
  lazy val baseURL = config.baseUrl("external-business-rates-data-platform")
  lazy val url = baseURL + "/dvr-case-management-api"

  def requestDetailedValuation(request: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    ws.POST[DetailedValuationRequest, HttpResponse](url + "/dvr_case/create_dvr_case", request) map { _ => () }
  }
}
