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

import config.WSHttp
import javax.inject.Inject
import models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class CheckCaseConnector @Inject()(
                                    wSHttp: WSHttp,
                                    config: ServicesConfig
                                  ){
  lazy val baseUrl: String = config.baseUrl("external-business-rates-data-platform")
  lazy val voaModernisedApiStubBaseUrl: String = config.baseUrl("voa-modernised-api")


  def getCheckCases(submissionId: String, party: String)(implicit request: ModernisedEnrichedRequest[_]): Future[Option[CheckCasesResponse]] = {
     implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      .withExtraHeaders("GG-EXTERNAL-ID" -> request.externalId)
      .withExtraHeaders("GG-GROUP-ID" -> request.groupId)

    party match {
      case "agent"  =>  wSHttp.GET[Option[AgentCheckCasesResponse]](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/clients/all/property-links/$submissionId/check-cases?start=1&size=100") recover { case _ => None }
      case "client" =>  wSHttp.GET[Option[OwnerCheckCasesResponse]](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/property-links/$submissionId/check-cases?start=1&size=100") recover { case _ => None }
      case _        =>  Future.successful(None)

    }

  }

  def canChallenge(propertyLinkSubmissionId: String,
                   checkCaseRef: String,
                   valuationId: Long,
                   party: String)(implicit request: ModernisedEnrichedRequest[_]): Future[Option[CanChallengeResponse]] = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
      .withExtraHeaders("GG-EXTERNAL-ID" -> request.externalId)
      .withExtraHeaders("GG-GROUP-ID" -> request.groupId)

    party match {
      case "client"   =>  wSHttp.GET[HttpResponse](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId").map{ resp =>
        handleCanChallengeResponse(resp)
      } recover { case _ => None }
      case "agent"    =>  wSHttp.GET[HttpResponse](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/clients/all/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId").map{ resp =>
        handleCanChallengeResponse(resp)
      } recover { case _ => None }
      case _          =>  throw new IllegalArgumentException(s"Unknown party $party")

    }
  }

  private def handleCanChallengeResponse(resp: HttpResponse): Option[CanChallengeResponse] = {
    resp.status match {
      case 200 => Json.parse(resp.body).asOpt[CanChallengeResponse]
      case _   => None
    }
  }
}
