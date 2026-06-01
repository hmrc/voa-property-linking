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

import models.modernised.ValuationHistoryResponse
import models.modernised.externalvaluationmanagement.documents.DvrDocumentFiles
import play.api.http.HeaderNames._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.ModernisedRequestErrorLogging
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import java.net.URL
import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ModernisedExternalValuationManagementApi @Inject() (
      httpClientV2: HttpClientV2,
      httpClient: VoaHttpClient,
      @Named("voa.modernised.authValuationHistoryUrl") valuationHistoryUrl: String,
      config: ServicesConfig,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector with ModernisedRequestErrorLogging {

  lazy val appName: String = config.getConfString("appName", "voa-property-linking")

  lazy val url: String = s"${appConfig.modernisedBase}/external-valuation-management-api"

  def getDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[Option[DvrDocumentFiles]] = {
    val dvrUrl = s"$url/properties/$uarn/valuations/$valuationId/files?propertyLinkId=$propertyLinkId"
    val response = httpClient
      .getWithGGHeaders[Option[DvrDocumentFiles]](
        url = dvrUrl
      )
    logModernisedErrorResponse(
      response,
      Seq("valuationId" -> valuationId.toString, "uarn" -> uarn.toString, "propertyLinkId" -> propertyLinkId),
      dvrUrl
    )(request.principal, executionContext)
  }

  def getValuationHistory(uarn: Long, propertyLinkSubmissionId: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[Option[ValuationHistoryResponse]] = {
    val historyUrl = valuationHistoryUrl.replace("{uarn}", uarn.toString)
    val fullUrl = s"$historyUrl?propertyLinkId=$propertyLinkSubmissionId"
    val response = httpClient
      .getWithGGHeaders[Option[ValuationHistoryResponse]](fullUrl)
    logModernisedErrorResponse(
      response,
      Seq("uarn" -> uarn.toString, "propertyLinkSubmissionId" -> propertyLinkSubmissionId),
      fullUrl
    )(request.principal, executionContext)
  }

  def getDvrDocument(valuationId: Long, uarn: Long, propertyLinkId: String, fileRef: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[HttpResponse] = {
    val dvrUrl = s"$url/properties/$uarn/valuations/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId"
    val response = httpClientV2
      .get(new URL(dvrUrl))
      .setHeader(
        Seq(
          "GG-EXTERNAL-ID"            -> request.principal.externalId,
          USER_AGENT                  -> appName,
          "GG-GROUP-ID"               -> request.principal.groupId,
          "Ocp-Apim-Subscription-Key" -> appConfig.apimSubscriptionKeyValue
        ): _*
      )
      .withProxy
      .stream
      .flatMap { result =>
        result.status match {
          case s if is4xx(s) || is5xx(s) =>
            val errorResponse = UpstreamErrorResponse(s"Upload failed with status ${result.status}.", s, s)
            logModernisedErrorResponse(
              Future.failed(errorResponse),
              Seq(
                "valuationId"    -> valuationId.toString,
                "uarn"           -> uarn.toString,
                "fileRef"        -> fileRef,
                "propertyLinkId" -> propertyLinkId
              ),
              dvrUrl
            )(request.principal, executionContext)
          case _ => Future.successful(result)
        }
      }
    response
  }
}
