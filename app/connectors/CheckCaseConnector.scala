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

import config.WSHttp
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.inject.ServicesConfig

import scala.concurrent.Future

class CheckCaseConnector @Inject()(config: ServicesConfig){
  lazy val baseUrl: String = config.baseUrl("external-business-rates-data-platform")


  def getCheckCases(submissionId: String, party: String)(implicit request: ModernisedEnrichedRequest[_]): Future[Option[CheckCasesResponse]] = {
     implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      .withExtraHeaders("GG-EXTERNAL-ID" -> request.externalId)
      .withExtraHeaders("GG-GROUP-ID" -> request.groupId)

    party match {
      case "agent"  =>  WSHttp.GET[Option[AgentCheckCasesResponse]](s"$baseUrl/external-case-management-api/my-organisation/clients/all/property-links/$submissionId/check-cases?start=1&size=100") recover { case _: NotFoundException => None }
      case "client" =>  WSHttp.GET[Option[OwnerCheckCasesResponse]](s"$baseUrl/external-case-management-api/my-organisation/property-links/$submissionId/check-cases?start=1&size=100") recover { case _: NotFoundException => None }
      case _       =>  Future.successful(None)

    }

  }
}
