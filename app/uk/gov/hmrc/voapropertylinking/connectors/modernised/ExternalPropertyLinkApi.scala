/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import models.PaginationParams
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, ClientsResponse, PropertyLinksWithClient}
import models.modernised.externalpropertylink.myorganisations.{AgentList, OwnerPropertyLink, PropertyLinksWithAgents}
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import uk.gov.hmrc.http._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import scala.concurrent.{ExecutionContext, Future}

class ExternalPropertyLinkApi @Inject()(
      val http: VoaHttpClient,
      @Named("voa.myAgentPropertyLinks") myAgentPropertyLinksUrl: String,
      @Named("voa.myOrganisationsPropertyLinks") myOrganisationsPropertyLinksUrl: String,
      @Named("voa.myOrganisationsPropertyLink") myOrganisationsPropertyLinkUrl: String,
      @Named("voa.myClientsPropertyLinks") myClientsPropertyLinksUrl: String,
      @Named("voa.myClientsPropertyLink") myClientsPropertyLinkUrl: String,
      @Named("voa.createPropertyLink") createPropertyLinkUrl: String,
      @Named("voa.createPropertyLinkOnClientBehalf") createPropertyLinkOnClientBehalfUrl: String,
      @Named("voa.myOrganisationsAgents") myOrganisationsAgentsUrl: String,
      @Named("voa.revokeClientsPropertyLink") revokeClientsPropertyLinkUrl: String,
      @Named("voa.myClients") myClientsUrl: String
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: PaginationParams)(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents] =
    http.GET[PropertyLinksWithAgents](
      myAgentPropertyLinksUrl.replace("{agentCode}", agentCode.toString),
      modernisedPaginationParams(Some(params)) ++
        List(
          searchParams.address.map("address"     -> _),
          searchParams.uarn.map("uarn"           -> _.toString),
          searchParams.baref.map("baref"         -> _),
          searchParams.agent.map("agent"         -> _),
          searchParams.status.map("status"       -> _),
          searchParams.sortField.map("sortfield" -> _),
          searchParams.sortOrder.map("sortorder" -> _)
        ).flatten
    )

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: Option[PaginationParams])(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents] =
    http.GET[PropertyLinksWithAgents](
      myOrganisationsPropertyLinksUrl,
      modernisedPaginationParams(params) ++
        List(
          searchParams.address.map("address"     -> _),
          searchParams.uarn.map("uarn"           -> _.toString),
          searchParams.baref.map("baref"         -> _),
          searchParams.agent.map("agent"         -> _),
          searchParams.status.map("status"       -> _),
          searchParams.sortField.map("sortfield" -> _),
          searchParams.sortOrder.map("sortorder" -> _)
        ).flatten
    )

  def getMyOrganisationsPropertyLink(submissionId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Option[OwnerPropertyLink]] =
    http.GET[Option[OwnerPropertyLink]](myOrganisationsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))

  def getClientsPropertyLinks(searchParams: GetMyClientsPropertyLinkParameters, params: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]): Future[Option[PropertyLinksWithClient]] =
    http
      .GET[Option[PropertyLinksWithClient]](
        myClientsPropertyLinksUrl,
        modernisedPaginationParams(params) ++
          List(
            searchParams.address.map("address"                           -> _),
            searchParams.baref.map("baref"                               -> _),
            searchParams.client.map("client"                             -> _),
            searchParams.status.map("status"                             -> _),
            searchParams.sortField.map("sortfield"                       -> _),
            searchParams.sortOrder.map("sortorder"                       -> _),
            searchParams.representationStatus.map("representationStatus" -> _),
            searchParams.appointedFromDate.map("appointedFromDate"       -> _.toString),
            searchParams.appointedToDate.map("appointedToDate"           -> _.toString)
          ).flatten
      )

  def getClientsPropertyLink(submissionId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Option[ClientPropertyLink]] =
    http.GET[Option[ClientPropertyLink]](myClientsPropertyLinkUrl.replace("{propertyLinkId}", submissionId))

  def getMyClients(searchParams: GetClientsParameters, params: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]): Future[ClientsResponse] =
    http
      .GET[ClientsResponse](
        myClientsUrl,
        modernisedPaginationParams(params) ++
          List(
            searchParams.name.map("name"                           -> _),
            searchParams.appointedFromDate.map("appointedFromDate" -> _.toString),
            searchParams.appointedToDate.map("appointedToDate"     -> _.toString)
          ).flatten
      )

  def createPropertyLink(propertyLink: CreatePropertyLink)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[HttpResponse] =
    http
      .POST[CreatePropertyLink, HttpResponse](createPropertyLinkUrl, propertyLink, Seq())

  def createOnClientBehalf(propertyLink: CreatePropertyLink, clientId: Long)(
    implicit hc: HeaderCarrier,
    request: RequestWithPrincipal[_]): Future[HttpResponse] =
    http
      .POST[CreatePropertyLink, HttpResponse](createPropertyLinkOnClientBehalfUrl.templated("clientId" -> clientId), propertyLink, Seq())

  def getMyOrganisationsAgents()(implicit request: RequestWithPrincipal[_]): Future[AgentList] =
    http.GET[AgentList](myOrganisationsAgentsUrl, List("requestTotalRowCount" -> "true"))

  def revokeClientProperty(plSubmissionId: String)(implicit request: RequestWithPrincipal[_]): Future[Unit] =
    http.DELETE[HttpResponse](revokeClientsPropertyLinkUrl.templated("submissionId" -> plSubmissionId)).map(_ => ())

  private def modernisedPaginationParams(params: Option[PaginationParams]): Seq[(String, String)] =
    params.fold(Seq.empty[(String, String)]) { p =>
      Seq(
        "start"                -> p.startPoint.toString,
        "size"                 -> p.pageSize.toString,
        "requestTotalRowCount" -> "true"
      )
    }

}
