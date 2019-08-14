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

package connectors.externalpropertylink

import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import http.VoaHttpClient
import javax.inject.{Inject, Named}
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, PropertyLinksWithClient}
import models.modernised.externalpropertylink.myorganisations.{OwnerPropertyLink, PropertyLinksWithAgents}
import models.voa.propertylinking.requests.CreatePropertyLink
import models.PaginationParams
import play.api.Logger
import uk.gov.hmrc.http._
import uk.gov.voa.voapropertylinking.auth.RequestWithPrincipal

import scala.concurrent.{ExecutionContext, Future}


class ExternalPropertyLinkConnector @Inject()(
                                               @Named("VoaAuthedBackendHttp") val http: VoaHttpClient,
                                               @Named("voa.myOrganisationsPropertyLinks") myOrganisationsPropertyLinksUrl: String,
                                               @Named("voa.myOrganisationsPropertyLink") myOrganisationsPropertyLinkUrl: String,
                                               @Named("voa.myClientsPropertyLinks") myClientsPropertyLinksUrl: String,
                                               @Named("voa.myClientsPropertyLink") myClientsPropertyLinkUrl: String,
                                               @Named("voa.createPropertyLink") createPropertyLinkUrl: String
                                             )(implicit executionContext: ExecutionContext)
  extends JsonHttpReads with OptionHttpReads with RawReads {

  private val logger = Logger(this.getClass.getName)

  def getMyOrganisationsPropertyLinks(searchParams: GetMyOrganisationPropertyLinksParameters, params: Option[PaginationParams])
                                     (implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[PropertyLinksWithAgents]] = {

    http.GET[Option[PropertyLinksWithAgents]](
      myOrganisationsPropertyLinksUrl,
      modernisedPaginationParams(params) ++
        List(searchParams.address.map("address" -> _),
          searchParams.baref.map("baref" -> _),
          searchParams.agent.map("agent" -> _),
          searchParams.status.map("status" -> _),
          searchParams.sortField.map("sortfield" -> _),
          searchParams.sortOrder.map("sortorder" -> _)).flatten)
  }

  def getMyOrganisationsPropertyLink(submissionId: String)
                                    (implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[OwnerPropertyLink]] =
    http.GET[Option[OwnerPropertyLink]](myOrganisationsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))

  def getClientsPropertyLinks(searchParams: GetMyClientsPropertyLinkParameters, params: Option[PaginationParams])
                             (implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[PropertyLinksWithClient]] =
    http
      .GET[Option[PropertyLinksWithClient]](
      myClientsPropertyLinksUrl,
      modernisedPaginationParams(params) ++
        List(
          searchParams.address.map("address" -> _),
          searchParams.baref.map("baref" -> _),
          searchParams.client.map("client" -> _),
          searchParams.status.map("status" -> _),
          searchParams.sortField.map("sortfield" -> _),
          searchParams.sortOrder.map("sortorder" -> _),
          searchParams.representationStatus.map("representationStatus" -> _)
        ).flatten)

  def getClientsPropertyLink(submissionId: String)
                            (implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[ClientPropertyLink]] =
    http.GET[Option[ClientPropertyLink]](myClientsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))

  def createPropertyLink(propertyLink: CreatePropertyLink)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[HttpResponse] =
    http
      .POST[CreatePropertyLink, HttpResponse](createPropertyLinkUrl, propertyLink, Seq())

  private def modernisedPaginationParams(params: Option[PaginationParams]): Seq[(String, String)] =
    params.fold(Seq.empty[(String, String)]){ p =>
      Seq(
        "start" -> p.startPoint,
        "size"  -> p.pageSize,
        "requestTotalRowCount" -> "true"
      ).map {
        case (key, value) => (key, value.toString)
      }
    }

}
