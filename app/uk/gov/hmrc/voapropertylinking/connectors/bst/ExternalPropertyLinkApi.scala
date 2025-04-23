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

import models.PaginationParams
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, ClientsResponse, PropertyLinksWithClient}
import models.modernised.externalpropertylink.myorganisations.{AgentList, OwnerPropertyLink, PropertyLinksWithAgents}
import models.modernised.externalpropertylink.requests.{CreatePropertyLink, CreatePropertyLinkOnClientBehalf}
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ExternalPropertyLinkApi @Inject() (
      val httpClient: VoaHttpClient,
      @Named("voa.myAgentPropertyLinks") myAgentPropertyLinksUrl: String,
      @Named("voa.myAgentAvailablePropertyLinks") myAgentAvailablePropertyLinks: String,
      @Named("voa.myOrganisationsPropertyLinks") myOrganisationsPropertyLinksUrl: String,
      @Named("voa.myOrganisationsPropertyLink") myOrganisationsPropertyLinkUrl: String,
      @Named("voa.myClientsPropertyLinks") myClientsPropertyLinksUrl: String,
      @Named("voa.myClientPropertyLinks") myClientPropertyLinksUrl: String,
      @Named("voa.myClientsPropertyLink") myClientsPropertyLinkUrl: String,
      @Named("voa.createPropertyLink") createPropertyLinkUrl: String,
      @Named("voa.createPropertyLinkOnClientBehalf") createPropertyLinkOnClientBehalfUrl: String,
      @Named("voa.myOrganisationsAgents") myOrganisationsAgentsUrl: String,
      @Named("voa.revokeClientsPropertyLink") revokeClientsPropertyLinkUrl: String,
      @Named("voa.myClients") myClientsUrl: String
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector with PropertyLinkApi {

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: PaginationParams
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents] = {
    val queryParams = modernisedPaginationParams(Some(params)) ++
      List(
        searchParams.address.map("address" -> _),
        searchParams.uarn.map("uarn" -> _.toString),
        searchParams.baref.map("baref" -> _),
        searchParams.agent.map("agent" -> _),
        searchParams.status.map("status" -> _),
        searchParams.sortField.map("sortfield" -> _),
        searchParams.sortOrder.map("sortorder" -> _)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    httpClient.getWithGGHeaders[PropertyLinksWithAgents](
      s"${myAgentPropertyLinksUrl.replace("{agentCode}", agentCode.toString)}?$queryString"
    )
  }

  def getMyAgentAvailablePropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents] = {

    val queryParams = modernisedPaginationParams(params) ++
      List(
        searchParams.address.map("address" -> _),
        searchParams.agent.map("agent" -> _),
        searchParams.sortField.map("sortfield" -> _),
        searchParams.sortOrder.map("sortorder" -> _)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    httpClient.getWithGGHeaders[PropertyLinksWithAgents](
      s"${myAgentAvailablePropertyLinks.replace("{agentCode}", agentCode.toString)}?$queryString"
    )
  }

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents] = {
    val queryParams = modernisedPaginationParams(params) ++
      List(
        searchParams.address.map("address" -> _),
        searchParams.uarn.map("uarn" -> _.toString),
        searchParams.baref.map("baref" -> _),
        searchParams.agent.map("agent" -> _),
        searchParams.status.map("status" -> _),
        searchParams.sortField.map("sortfield" -> _),
        searchParams.sortOrder.map("sortorder" -> _)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    httpClient.getWithGGHeaders[PropertyLinksWithAgents](
      s"$myOrganisationsPropertyLinksUrl?$queryString"
    )
  }

  def getMyOrganisationsPropertyLink(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): Future[Option[OwnerPropertyLink]] =
    httpClient.getWithGGHeaders[Option[OwnerPropertyLink]](
      myOrganisationsPropertyLinkUrl.replace("{propertyLinkId}", submissionId)
    )

  def getClientsPropertyLinks(searchParams: GetMyClientsPropertyLinkParameters, params: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]
  ): Future[Option[PropertyLinksWithClient]] = {

    val queryParams = modernisedPaginationParams(params) ++
      List(
        searchParams.address.map("address" -> _),
        searchParams.baref.map("baref" -> _),
        searchParams.client.map("client" -> _),
        searchParams.status.map("status" -> _),
        searchParams.sortField.map("sortfield" -> _),
        searchParams.sortOrder.map("sortorder" -> _),
        searchParams.representationStatus.map("representationStatus" -> _),
        searchParams.appointedFromDate.map("appointedFromDate" -> _.toString),
        searchParams.appointedToDate.map("appointedToDate" -> _.toString)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    httpClient
      .getWithGGHeaders[Option[PropertyLinksWithClient]](
        s"$myClientsPropertyLinksUrl?$queryString"
      )
  }

  def getClientPropertyLinks(
        clientOrgId: Long,
        searchParams: GetClientPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[Option[PropertyLinksWithClient]] = {

    val queryParams = modernisedPaginationParams(params) ++
      List(
        searchParams.address.map("address" -> _),
        searchParams.baref.map("baref" -> _),
        searchParams.status.map("status" -> _),
        searchParams.sortField.map("sortfield" -> _),
        searchParams.sortOrder.map("sortorder" -> _),
        searchParams.representationStatus.map("representationStatus" -> _),
        searchParams.appointedFromDate.map("appointedFromDate" -> _.toString),
        searchParams.appointedToDate.map("appointedToDate" -> _.toString),
        searchParams.uarn.map("uarn" -> _.toString),
        searchParams.client.map("client" -> _)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    httpClient
      .getWithGGHeaders[Option[PropertyLinksWithClient]](
        s"${myClientPropertyLinksUrl.replace("{clientId}", clientOrgId.toString)}?$queryString"
      )
  }

  def getClientsPropertyLink(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): Future[Option[ClientPropertyLink]] =
    httpClient.getWithGGHeaders[Option[ClientPropertyLink]](
      myClientsPropertyLinkUrl.replace("{propertyLinkId}", submissionId)
    )

  def getMyClients(searchParams: GetClientsParameters, params: Option[PaginationParams])(implicit
        request: RequestWithPrincipal[_]
  ): Future[ClientsResponse] = {

    val queryParams = modernisedPaginationParams(params) ++
      List(
        searchParams.name.map("name" -> _),
        searchParams.appointedFromDate.map("appointedFromDate" -> _.toString),
        searchParams.appointedToDate.map("appointedToDate" -> _.toString)
      ).flatten

    val queryString =
      queryParams.map { case (key, value) => s"$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }.mkString("&")

    val updatedParams = if (queryString.nonEmpty) s"?$queryString" else ""

    httpClient
      .getWithGGHeaders[ClientsResponse](s"$myClientsUrl$updatedParams")
  }

  def createPropertyLink(
        propertyLink: CreatePropertyLink
  )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[HttpResponse] =
    httpClient
      .postWithGgHeaders[HttpResponse](createPropertyLinkUrl, Json.toJsObject(propertyLink))

  def createOnClientBehalf(propertyLink: CreatePropertyLinkOnClientBehalf, clientId: Long)(implicit
        hc: HeaderCarrier,
        request: RequestWithPrincipal[_]
  ): Future[HttpResponse] =
    httpClient
      .postWithGgHeaders[HttpResponse](
        createPropertyLinkOnClientBehalfUrl.templated("clientId" -> clientId),
        Json.toJsObject(propertyLink)
      )

  def getMyOrganisationsAgents()(implicit request: RequestWithPrincipal[_]): Future[AgentList] =
    httpClient.getWithGGHeaders[AgentList](s"$myOrganisationsAgentsUrl?requestTotalRowCount=true")

  def revokeClientProperty(plSubmissionId: String)(implicit request: RequestWithPrincipal[_]): Future[Unit] = {
    httpClient
      .deleteWithGgHeaders[HttpResponse](revokeClientsPropertyLinkUrl.templated("submissionId" -> plSubmissionId))
      .map(_ => ())
  }

  private def modernisedPaginationParams(params: Option[PaginationParams]): Seq[(String, String)] =
    params.fold(Seq.empty[(String, String)]) { p =>
      Seq(
        "start"                -> p.startPoint.toString,
        "size"                 -> p.pageSize.toString,
        "requestTotalRowCount" -> "true"
      )
    }
}
