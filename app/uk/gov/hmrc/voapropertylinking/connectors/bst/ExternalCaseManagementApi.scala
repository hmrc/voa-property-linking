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

package uk.gov.hmrc.voapropertylinking.connectors.bst

import models._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myclients.CheckCasesWithClient
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myorganisation.CheckCasesWithAgent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/*
  TODO this connector should be moved to check backend after the planned migration to external-case-management-api
 */
class ExternalCaseManagementApi @Inject() (
      httpClient: VoaHttpClient,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  val queryParams = "?start=1&size=100"
  def getMyOrganisationCheckCases(
        propertyLinkSubmissionId: String
  )(implicit request: RequestWithPrincipal[_]): Future[CheckCasesWithAgent] =
    httpClient.getWithGGHeaders[CheckCasesWithAgent](
      s"${appConfig.bstBase}/external-case-management-api/my-organisation/property-links/$propertyLinkSubmissionId/check-cases$queryParams"
    )

  def getMyClientsCheckCases(
        propertyLinkSubmissionId: String
  )(implicit request: RequestWithPrincipal[_]): Future[CheckCasesWithClient] =
    httpClient.getWithGGHeaders[CheckCasesWithClient](
      s"${appConfig.bstBase}/external-case-management-api/my-organisation/clients/all/property-links/$propertyLinkSubmissionId/check-cases$queryParams"
    )

  def canChallenge(propertyLinkSubmissionId: String, checkCaseRef: String, valuationId: Long, party: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[Option[CanChallengeResponse]] =
    party match {
      case "client" =>
        httpClient
          .getWithGGHeaders[HttpResponse](
            s"${appConfig.bstBase}/external-case-management-api/my-organisation/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId"
          )
          .map(handleCanChallengeResponse) recover toNone
      case "agent" =>
        httpClient
          .getWithGGHeaders[HttpResponse](
            s"${appConfig.bstBase}/external-case-management-api/my-organisation/clients/all/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge?valuationId=$valuationId"
          )
          .map(handleCanChallengeResponse) recover toNone
      case _ => throw new IllegalArgumentException(s"Unknown party $party")
    }

  private def handleCanChallengeResponse(resp: HttpResponse): Option[CanChallengeResponse] =
    resp.status match {
      case 200 => Json.parse(resp.body).asOpt[CanChallengeResponse]
      case _   => None
    }
}
