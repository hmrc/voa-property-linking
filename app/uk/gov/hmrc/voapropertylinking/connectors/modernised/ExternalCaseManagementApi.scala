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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import javax.inject.Inject
import models.{AgentCheckCasesResponse, CanChallengeResponse, CheckCasesResponse, OwnerCheckCasesResponse}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import scala.concurrent.{ExecutionContext, Future}

class ExternalCaseManagementApi @Inject()(
                                           http: VoaHttpClient,
                                           servicesConfig: ServicesConfig
                                         )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform")
  lazy val voaModernisedApiStubBaseUrl: String = servicesConfig.baseUrl("voa-modernised-api")


  def getCheckCases(submissionId: String, party: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[CheckCasesResponse]] = {
    party match {
      case "agent" => http.GET[Option[AgentCheckCasesResponse]](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/clients/all/property-links/$submissionId/check-cases?start=1&size=100") recover { case _ => None }
      case "client" => http.GET[Option[OwnerCheckCasesResponse]](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/property-links/$submissionId/check-cases?start=1&size=100") recover { case _ => None }
      case _ => Future.successful(None)
    }
  }

  def canChallenge(propertyLinkSubmissionId: String,
                   checkCaseRef: String,
                   valuationId: Long,
                   party: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[CanChallengeResponse]] = {
    party match {
      case "client" => http.GET[HttpResponse](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId").map { resp =>
        handleCanChallengeResponse(resp)
      } recover { case _ => None }
      case "agent" => http.GET[HttpResponse](s"$voaModernisedApiStubBaseUrl/external-case-management-api/my-organisation/clients/all/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId").map { resp =>
        handleCanChallengeResponse(resp)
      } recover { case _ => None }
      case _ => throw new IllegalArgumentException(s"Unknown party $party")

    }
  }

  private def handleCanChallengeResponse(resp: HttpResponse): Option[CanChallengeResponse] = {
    resp.status match {
      case 200 => Json.parse(resp.body).asOpt[CanChallengeResponse]
      case _ => None
    }
  }
}
