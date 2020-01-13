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

package uk.gov.hmrc.voapropertylinking.connectors.mdtp

import javax.inject.Inject
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.{ExecutionContext, Future}

class BusinessRatesAuthConnector @Inject()(
                                            http: DefaultHttpClient,
                                            servicesConfig: ServicesConfig
                                          )(implicit executionContext: ExecutionContext) extends BaseMdtpConnector {

  lazy val baseUrl = servicesConfig.baseUrl("business-rates-auth")
  lazy val url = baseUrl + "/business-rates-authorisation"

  def clearCache()(implicit hc: HeaderCarrier): Future[Unit] = {
    http.DELETE[HttpResponse](url + "/cache") map { _ => () }
  }
}
