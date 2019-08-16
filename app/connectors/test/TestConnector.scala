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

package connectors.test

import javax.inject.{Inject, Named}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.voa.voapropertylinking.auth.RequestWithPrincipal

import scala.concurrent.{ExecutionContext, Future}

class TestConnector @Inject()(
                               @Named("VoaBackendWsHttp") http: WSHttp,
                               conf: ServicesConfig
                             )(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = conf.baseUrl("external-business-rates-data-platform")
  lazy val url = baseUrl + "/test-only/customer-management-api/organisation"

  def deleteOrganisation(orgId: Long)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    http.DELETE[HttpResponse](s"$url?organisationId=$orgId")
  }

  def deleteCheckCases(propertyLinkingSubmissionId: String)(implicit request: RequestWithPrincipal[_]): Future[HttpResponse] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      .withExtraHeaders("GG-EXTERNAL-ID" -> request.principal.externalId)
      .withExtraHeaders("GG-GROUP-ID" -> request.principal.groupId)

    http.DELETE[HttpResponse](s"$baseUrl/external-case-management-api/check-cases/$propertyLinkingSubmissionId")
  }

}