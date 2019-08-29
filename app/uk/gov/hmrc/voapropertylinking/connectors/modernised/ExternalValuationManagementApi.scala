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

import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import javax.inject.{Inject, Named}
import models.modernised.ValuationHistoryResponse
import models.voa.valuation.dvr.StreamedDocument
import models.voa.valuation.dvr.documents.DvrDocumentFiles
import play.api.http.HeaderNames.{CONTENT_LENGTH, CONTENT_TYPE, _}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{StreamedResponse, WSClient}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal

import scala.concurrent.Future


class ExternalValuationManagementApi @Inject()(
                                      wsClient: WSClient,
                                      http: VoaHttpClient,
                                      @Named("voa.authValuationHistoryUrl") valuationHistoryUrl: String,
                                      config: ServicesConfig
                                    ) extends BaseVoaConnector {

  lazy val appName = config.getConfString("appName", "voa-property-linking")

  lazy val baseURL = config.baseUrl("external-business-rates-data-platform")
  lazy val url = baseURL + "/external-valuation-management-api"

  def getDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[DvrDocumentFiles]] =
    http.GET[DvrDocumentFiles](s"$url/properties/$uarn/valuations/$valuationId/files", Seq("propertyLinkId" -> propertyLinkId))
      .map(Option.apply)
      .recover {
        case _: NotFoundException  =>
          None
        case e                     =>
          throw e
      }

  def getValuationHistory(uarn: Long, propertyLinkSubmissionId: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[ValuationHistoryResponse]] = {
    http
      .GET[Option[ValuationHistoryResponse]](
      valuationHistoryUrl.replace("{uarn}", uarn.toString),
        modernisedValuationHistoryQueryParameters(propertyLinkSubmissionId))
  }

  def getDvrDocument(
                      valuationId: Long,
                      uarn: Long,
                      propertyLinkId: String,
                      fileRef: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[StreamedDocument] =
    wsClient
      .url(s"$url/properties/$uarn/valuations/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId")
      .withMethod("GET")
      .withHeaders(List(
        "GG-EXTERNAL-ID" -> request.principal.externalId,
        USER_AGENT -> appName,
        "GG-GROUP-ID"    -> request.principal.groupId): _*)
      .stream().flatMap {
      case StreamedResponse(hs, body) =>

        val headers = hs.headers.mapValues(_.mkString(","))
        hs.status match {
          case s if is4xx(s) => Future.failed(Upstream4xxResponse(s"Upload failed with status ${hs.status}.", s, s))
          case s if is5xx(s) => Future.failed(Upstream5xxResponse(s"Upload failed with status ${hs.status}.", s, s))
          case _ => Future.successful(
            StreamedDocument(headers.get(CONTENT_TYPE), headers.get(CONTENT_LENGTH).map(_.toLong), headers.filter(_._1 != CONTENT_TYPE), body)
          )
        }
    }

  private def modernisedValuationHistoryQueryParameters(propertyLinkSubmissionId: String) =
    Seq("propertyLinkId" -> propertyLinkSubmissionId)
}