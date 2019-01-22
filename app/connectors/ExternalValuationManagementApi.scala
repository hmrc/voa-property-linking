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

import http.VoaHttpClient
import javax.inject.{Inject, Named}
import models.ModernisedEnrichedRequest
import models.dvr.StreamedDocument
import models.dvr.documents.DvrDocumentFiles
import play.api.http.HeaderNames.{CONTENT_LENGTH, CONTENT_TYPE}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.{StreamedResponse, WSClient}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future


class ExternalValuationManagementApi @Inject()(
                                      wsClient: WSClient,
                                      @Named("VoaBackendWsHttp") ws: WSHttp,
                                      @Named("VoaAuthedBackendHttp") http: VoaHttpClient
                                    ) extends ServicesConfig with HttpErrorFunctions {
  lazy val baseURL = baseUrl("external-business-rates-data-platform")
  lazy val url = baseURL + "/external-valuation-management-api"

  def getDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[Option[DvrDocumentFiles]] =
    http.GET[DvrDocumentFiles](s"$url/properties/$uarn/valuations/$valuationId/files", Seq("propertyLinkId" -> propertyLinkId))
      .map(Option.apply)
      .recover {
        case _: NotFoundException  =>
          None
        case e                     =>
          throw e
      }

  def getDvrDocument(
                      valuationId: Long,
                      uarn: Long,
                      propertyLinkId: String,
                      fileRef: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[StreamedDocument] =
    wsClient
      .url(s"$url/properties/$uarn/valuations/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId")
      .withMethod("GET")
      .withHeaders(hc.withExtraHeaders(
        "GG-EXTERNAL-ID" -> request.externalId,
        "GG-GROUP-ID"    -> request.groupId).extraHeaders: _*)
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
}