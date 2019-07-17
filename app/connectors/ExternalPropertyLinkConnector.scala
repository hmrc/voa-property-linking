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

import binders.GetPropertyLinksParameters
import http.VoaHttpClientAuth
import http.VoaHttpClient
import javax.inject.{Inject, Named}
import models.modernised.{ClientPropertyLink, CreatePropertyLink, OwnerPropertyLink, PropertyLinksWithAgents, PropertyLinksWithClient}
import uk.gov.hmrc.http._
import models.ModernisedEnrichedRequest
import uk.gov.hmrc.play.config.ServicesConfig
import models.PaginationParams

import scala.concurrent.{ExecutionContext, Future}


class ExternalPropertyLinkConnector @Inject()
                                              (@Named("VoaAuthedBackendHttp") val http: VoaHttpClient,
                                              @Named("voa.myOrganisationsPropertyLinks") myOrganisationsPropertyLinksUrl: String,
                                              @Named("voa.myOrganisationsPropertyLink") myOrganisationsPropertyLinkUrl: String,
                                              @Named("voa.myClientsPropertyLinks") myClientsPropertyLinksUrl: String,
                                              @Named("voa.myClientsPropertyLink") myClientsPropertyLinkUrl: String,
                                              @Named("voa.createPropertyLink") createPropertyLinkUrl: String,
                                               conf: ServicesConfig) (implicit executionContext: ExecutionContext)
  extends JsonHttpReads with OptionHttpReads with RawReads {




  def getMyOrganisationsPropertyLinks(searchParams: GetPropertyLinksParameters, params: Option[PaginationParams])
                                     (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[Option[PropertyLinksWithAgents]] = {

    http.GET[Option[PropertyLinksWithAgents]](
      myOrganisationsPropertyLinksUrl,
      modernisedPaginationParams(params) ++
        List(searchParams.address.map("address" -> _),
          searchParams.baref.map("baref" -> _),
          searchParams.agent.map("agent" -> _),
          searchParams.status.map("status" -> _),
          searchParams.sortfield.map("sortfield" -> _),
          searchParams.sortorder.map("sortorder" -> _)).flatten)
  }


  def getMyOrganisationsPropertyLink(submissionId: String)
                                    (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[Option[OwnerPropertyLink]] =
    http.GET[Option[OwnerPropertyLink]](myOrganisationsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))

  def getClientsPropertyLinks(searchParams: GetPropertyLinksParameters, params: Option[PaginationParams])
                             (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[Option[PropertyLinksWithClient]] =
    http
      .GET[Option[PropertyLinksWithClient]](
      myClientsPropertyLinksUrl,
      modernisedPaginationParams(params) ++
        List(searchParams.address.map("address" -> _),
          searchParams.baref.map("baref" -> _),
          searchParams.agent.map("agent" -> _),
          searchParams.status.map("status" -> _),
          searchParams.sortfield.map("sortfield" -> _),
          searchParams.sortorder.map("sortorder" -> _)).flatten)


  def getClientsPropertyLink(submissionId: String)
                            (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[Option[ClientPropertyLink]] =
    http.GET[Option[ClientPropertyLink]](myClientsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))


  def createPropertyLink(propertyLink: CreatePropertyLink)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[HttpResponse] =
    http
      .POST[CreatePropertyLink, HttpResponse](createPropertyLinkUrl, propertyLink, Seq())


  private def modernisedPaginationParams(params: Option[PaginationParams]): Seq[(String, String)] =
    params match {
      case Some(i) => Seq("start" -> calculateStart(i),
                          "size"  -> i.pageSize,
                          "requestTotalRowCount" -> "true").map({ case (key, value) => (key, value.toString) })
      case None => Seq()
    }

  private def calculateStart(params: PaginationParams): Int = ((params.startPoint - 1) * params.pageSize) + 1

}
