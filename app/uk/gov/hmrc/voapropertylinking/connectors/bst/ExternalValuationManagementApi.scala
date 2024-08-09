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

import models.modernised.ValuationHistoryResponse
import models.modernised.externalvaluationmanagement.documents.DvrDocumentFiles
import play.api.http.HeaderNames._
import play.api.libs.ws.{WSClient, WSResponse}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ExternalValuationManagementApi @Inject()(
      wsClient: WSClient,
      http: VoaHttpClient,
      @Named("voa.authValuationHistoryUrl") valuationHistoryUrl: String,
      config: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val appName: String = config.getConfString("appName", "voa-property-linking")

  lazy val url: String = config.baseUrl("voa-bst") + "/external-valuation-management-api"

  def getDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Option[DvrDocumentFiles]] =
    http
      .GET[Option[DvrDocumentFiles]](
        s"$url/properties/$uarn/valuations/$valuationId/files",
        Seq("propertyLinkId" -> propertyLinkId))

  def getValuationHistory(uarn: Long, propertyLinkSubmissionId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Option[ValuationHistoryResponse]] =
    http
      .GET[Option[ValuationHistoryResponse]](
        valuationHistoryUrl.replace("{uarn}", uarn.toString),
        modernisedValuationHistoryQueryParameters(propertyLinkSubmissionId))

  def getDvrDocument(valuationId: Long, uarn: Long, propertyLinkId: String, fileRef: String)(
        implicit request: RequestWithPrincipal[_]): Future[WSResponse] =
    wsClient
      .url(s"$url/properties/$uarn/valuations/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId")
      .withMethod("GET")
      .withHttpHeaders(
        List(
          "GG-EXTERNAL-ID" -> request.principal.externalId,
          USER_AGENT       -> appName,
          "GG-GROUP-ID"    -> request.principal.groupId): _*)
      .stream()
      .flatMap { response =>
        response.status match {
          case s if is4xx(s) || is5xx(s) =>
            Future.failed(UpstreamErrorResponse(s"Upload failed with status ${response.status}.", s, s))
          case _ => Future.successful(response)
        }
      }

  private def modernisedValuationHistoryQueryParameters(propertyLinkSubmissionId: String) =
    Seq("propertyLinkId" -> propertyLinkSubmissionId)
}
