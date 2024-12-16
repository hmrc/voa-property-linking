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
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}

import scala.concurrent.Future

trait PropertyLinkApi {

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: PaginationParams
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents]

  def getMyAgentAvailablePropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents]

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[PropertyLinksWithAgents]

  def getMyOrganisationsPropertyLink(submissionId: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[Option[OwnerPropertyLink]]

  def getClientsPropertyLinks(searchParams: GetMyClientsPropertyLinkParameters, params: Option[PaginationParams])(
        implicit request: RequestWithPrincipal[_]
  ): Future[Option[PropertyLinksWithClient]]
  def getClientPropertyLinks(
        clientOrgId: Long,
        searchParams: GetClientPropertyLinksParameters,
        params: Option[PaginationParams]
  )(implicit request: RequestWithPrincipal[_]): Future[Option[PropertyLinksWithClient]]

  def getClientsPropertyLink(submissionId: String)(implicit
        request: RequestWithPrincipal[_]
  ): Future[Option[ClientPropertyLink]]

  def getMyClients(searchParams: GetClientsParameters, params: Option[PaginationParams])(implicit
        request: RequestWithPrincipal[_]
  ): Future[ClientsResponse]

  def createPropertyLink(
        propertyLink: CreatePropertyLink
  )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[HttpResponse]

  def createOnClientBehalf(propertyLink: CreatePropertyLinkOnClientBehalf, clientId: Long)(implicit
        hc: HeaderCarrier,
        request: RequestWithPrincipal[_]
  ): Future[HttpResponse]

  def getMyOrganisationsAgents()(implicit request: RequestWithPrincipal[_]): Future[AgentList]

  def revokeClientProperty(plSubmissionId: String)(implicit request: RequestWithPrincipal[_]): Future[Unit]

}
