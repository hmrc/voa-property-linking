/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps}
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesDashboardFrontendConnector.readUnit

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessRatesDashboardFrontendConnector @Inject() (
      httpClient: HttpClientV2,
      appConfig: AppConfig
)(implicit ec: ExecutionContext) {
  def invalidateAgentHasClientsCache(agentCode: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = url"${appConfig.dashboardFrontendBase}/internal/cache/agentHasClientsCache/$agentCode/invalidate/"

    httpClient
      .post(url)
      .setHeader("X-Cache-Endpoint-Secret" -> appConfig.dashboardFrontendAgentHasClientsCacheSecret)
      .execute[Unit]
  }
}

object BusinessRatesDashboardFrontendConnector {
  implicit val readUnit: HttpReads[Unit] =
    HttpReads[HttpResponse].map(_ => ())
}
