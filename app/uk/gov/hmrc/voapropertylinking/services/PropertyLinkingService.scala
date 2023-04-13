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

package uk.gov.hmrc.voapropertylinking.services

import cats.data.OptionT
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.projections.OwnerAuthResult
import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, ClientsResponse}
import models.modernised.externalpropertylink.myorganisations.AgentList
import models.modernised.externalpropertylink.requests.{CreatePropertyLink, CreatePropertyLinkOnClientBehalf}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.{ExternalPropertyLinkApi, PropertyLinkApi}
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ModernisedExternalPropertyLinkApi
import uk.gov.hmrc.voapropertylinking.utils.Cats

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingService @Inject()(
      val modernisedPropertyLinksConnector: ModernisedExternalPropertyLinkApi,
      propertyLinksConnector: ExternalPropertyLinkApi,
      featureSwitch: FeatureSwitch
)(implicit executionContext: ExecutionContext)
    extends Cats {

  protected def connector: PropertyLinkApi =
    if (featureSwitch.isBstDownstreamEnabled) propertyLinksConnector else modernisedPropertyLinksConnector

  def create(propertyLink: APIPropertyLinkRequest)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[HttpResponse] =
    connector.createPropertyLink(CreatePropertyLink(propertyLink))

  def createOnClientBehalf(propertyLink: APIPropertyLinkRequest, clientId: Long)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[HttpResponse] =
    connector.createOnClientBehalf(CreatePropertyLinkOnClientBehalf(propertyLink), clientId)

  def getClientsPropertyLink(submissionId: String)(
        implicit request: RequestWithPrincipal[_]): OptionT[Future, ClientPropertyLink] =
    OptionT(connector.getClientsPropertyLink(submissionId))

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: PaginationParams)(implicit request: RequestWithPrincipal[_]): Future[OwnerAuthResult] =
    connector
      .getMyAgentPropertyLinks(agentCode, searchParams, paginationParams)
      .map(OwnerAuthResult.apply)

  def getMyAgentAvailablePropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]): Future[OwnerAuthResult] =
    connector
      .getMyAgentAvailablePropertyLinks(agentCode, searchParams, paginationParams)
      .map(OwnerAuthResult.apply)

  def getMyOrganisationsPropertyLink(submissionId: String)(
        implicit request: RequestWithPrincipal[_]): OptionT[Future, PropertiesView] =
    OptionT(connector.getMyOrganisationsPropertyLink(submissionId)).map(pl => PropertiesView(pl.authorisation, Nil))

  def getClientsPropertyLinks(
        searchParams: GetMyClientsPropertyLinkParameters,
        paginationParams: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, PropertyLinksWithClients] =
    OptionT(connector.getClientsPropertyLinks(searchParams, paginationParams))
      .map(PropertyLinksWithClients.apply)

  def getClientPropertyLinks(
        clientId: Long,
        searchParams: GetClientPropertyLinksParameters,
        paginationParams: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, PropertyLinksWithClients] =
    OptionT(connector.getClientPropertyLinks(clientId, searchParams, paginationParams))
      .map(PropertyLinksWithClients.apply)

  def getMyClients(
        searchParams: GetClientsParameters,
        paginationParams: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[ClientsResponse] =
    connector.getMyClients(searchParams, paginationParams)

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]): Future[OwnerAuthResult] =
    connector
      .getMyOrganisationsPropertyLinks(searchParams, paginationParams)
      .map(OwnerAuthResult.apply)

  def getMyOrganisationsPropertyLinksCount()(implicit request: RequestWithPrincipal[_]): Future[Int] =
    connector
      .getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(), None)
      .map(propertyLinks => propertyLinks.filterTotal)

  def getMyOrganisationsAgents()(implicit request: RequestWithPrincipal[_]): Future[AgentList] =
    connector.getMyOrganisationsAgents()

}
