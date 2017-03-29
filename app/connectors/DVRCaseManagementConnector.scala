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

import com.google.inject.Singleton
import models.DetailedValuationRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.Future

@Singleton
class DVRCaseManagementConnector @Inject() (@Named("VoaBackendWsHttp") http: WSHttp) extends ServicesConfig {
  val url = baseUrl("external-business-rates-data-platform") + "/dvr-case-management-api"

  def requestDetailedValuation(request: DetailedValuationRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[DetailedValuationRequest, HttpResponse](url + "/dvr_case/create_dvr_case", request) map { _ => () }
  }
}
