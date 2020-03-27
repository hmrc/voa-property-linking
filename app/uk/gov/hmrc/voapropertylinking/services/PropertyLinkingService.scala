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

package uk.gov.hmrc.voapropertylinking.services

import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import cats.data.OptionT
import javax.inject.Inject

import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.projections.OwnerAuthResult
import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.externalpropertylink.myclients.ClientPropertyLink
import models.modernised.externalpropertylink.myorganisations.{AgentList, PropertyLinksWithAgents}
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{ExternalPropertyLinkApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingService @Inject()(
      val propertyLinksConnector: ExternalPropertyLinkApi,
      val externalValuationManagementApi: ExternalValuationManagementApi
)(implicit executionContext: ExecutionContext)
    extends Cats {

  def create(propertyLink: APIPropertyLinkRequest)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[HttpResponse] =
    propertyLinksConnector.createPropertyLink(CreatePropertyLink(propertyLink))

  def getClientsPropertyLink(submissionId: String)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): OptionT[Future, ClientPropertyLink] =
    OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: PaginationParams)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[OwnerAuthResult] =
    propertyLinksConnector
      .getMyAgentPropertyLinks(agentCode, searchParams, paginationParams)
      .map(OwnerAuthResult.apply)

  def getMyOrganisationsPropertyLink(submissionId: String)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): OptionT[Future, PropertiesView] =
    OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId)).map(pl =>
      PropertiesView(pl.authorisation, Nil))

  def getClientsPropertyLinks(
        searchParams: GetMyClientsPropertyLinkParameters,
        paginationParams: Option[PaginationParams]
  )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): OptionT[Future, PropertyLinksWithClients] =
    OptionT(propertyLinksConnector.getClientsPropertyLinks(searchParams, paginationParams))
      .map(PropertyLinksWithClients.apply)

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: Option[PaginationParams])(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[OwnerAuthResult] =
    propertyLinksConnector
      .getMyOrganisationsPropertyLinks(searchParams, paginationParams)
      .map(OwnerAuthResult.apply)

  def getMyOrganisationsPropertyLinksCount()(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[Int] =
    propertyLinksConnector
      .getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(), None)
      .map(propertyLinks => propertyLinks.filterTotal)

  def getMyOrganisationsAgents()(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[AgentList] =
    propertyLinksConnector.getMyOrganisationsAgents()

}
