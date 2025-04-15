/*
 * Copyright 2023 HM Revenue & Customs
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

import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ModernisedCCACaseManagementApi @Inject() (
      httpClient: VoaHttpClient,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val url = s"${appConfig.modernisedBase}/cca-case-management-api"

  def requestDetailedValuation(
        request: DetailedValuationRequest
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Unit] =
    httpClient
      .postWithGgHeaders[HttpResponse](url + "/cca_case/dvrSubmission", Json.toJsObject(request))
      .map { _ =>
        ()
      }
}
