/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import infrastructure.SimpleWSHttp
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAuthConnector @Inject()(http: SimpleWSHttp, servicesConfig: ServicesConfig) {

  lazy val baseUrl = servicesConfig.baseUrl("business-rates-auth")
  lazy val url = baseUrl + "/business-rates-authorisation"

  def clearCache()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    http.DELETE(url + "/cache") map { _ => () }
  }
}
