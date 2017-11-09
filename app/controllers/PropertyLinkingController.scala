/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import javax.inject.Inject

import auth.Authenticated
import connectors.auth.AuthConnector
import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import models._
import models.searchApi.AgentAuthResultFE
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}

import scala.collection.mutable
import scala.concurrent.Future

case class Memoize[K, V]() {
  private val cache = mutable.Map.empty[K, V]
  def apply(f: K => V): K => V = in => cache.getOrElseUpdate(in, f(in))
}

class PropertyLinkingController @Inject()(val auth: AuthConnector,
                                          propertyLinksConnector: PropertyLinkingConnector,
                                          groupAccountsConnector: GroupAccountConnector,
                                          representationsConnector: PropertyRepresentationConnector
                                         ) extends PropertyLinkingBaseController with Authenticated {
  type GroupCache = Memoize[Long, Future[Option[GroupAccount]]]

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      propertyLinksConnector.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
        .map { _ => Created }
        .recover { case _: Upstream5xxResponse => InternalServerError }
    }
  }

  def get(authorisationId: Long) = authenticated { implicit request =>
      implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

      propertyLinksConnector.get(authorisationId) flatMap {
        case Some(authorisation) => detailed(authorisation) map { d => Ok(Json.toJson(d)) }
        case None => NotFound
      }
  }

  def find(organisationId: Long, paginationParams: PaginationParams) = authenticated { implicit request =>
    getProperties(organisationId, paginationParams).map(x => Ok(Json.toJson(x)))
  }

  def searchAndSort(organisationId: Long,
                    paginationParams: PaginationParams,
                    sortfield: Option[String],
                    sortorder: Option[String],
                    status: Option[String],
                    address: Option[String],
                    baref: Option[String],
                    agent: Option[String]) = authenticated { implicit request =>

        propertyLinksConnector.searchAndSort(
          organisationId = organisationId,
          params = paginationParams,
          sortfield = sortfield,
          sortorder = sortorder,
          status = status,
          address = address,
          baref = baref,
          agent = agent).map(x => Ok(Json.toJson(x)))
  }


  /***
    * Make two calls to the Search/Sort API
    * the first call returns the results based on supplied filters and sortfield
    * the second call is used only to allow us to get the count of PENDING representation requests
    */
  def forAgentSearchAndSort(organisationId: Long,
                    paginationParams: PaginationParams,
                    sortfield: Option[String],
                    sortorder: Option[String],
                    status: Option[String],
                    address: Option[String],
                    baref: Option[String],
                    client: Option[String]) = authenticated { implicit request =>
    val eventualAuthResultBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      sortfield = sortfield,
      sortorder = sortorder,
      status = status,
      address = address,
      baref = baref,
      client = client,
      representationStatus = Some("APPROVED")) // TODO cater for other statuses

    // required to calculate the pending count - no filtering/sorting required
    val eventualAuthResultPendingBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      representationStatus = Some("PENDING"))

    // required to get the correct filtered amount - no filtering/sorting required
    val eventualAuthResultApprovedNoFiltersBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      representationStatus = Some("APPROVED"))

    for {
      authResultBE <- eventualAuthResultBE
      authResultPendingBE <- eventualAuthResultPendingBE
      authResultApprovedNoFiltersBE <- eventualAuthResultApprovedNoFiltersBE
    } yield Ok(Json.toJson(AgentAuthResultFE(authResultBE, authResultPendingBE.filterTotal, authResultApprovedNoFiltersBE.filterTotal)))

  }

  private def getProperties(organisationId: Long, params: PaginationParams)(implicit hc: HeaderCarrier): Future[PropertyLinkResponse] = {
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    for {
      view <- propertyLinksConnector.find(organisationId, params)
      detailedLinks <- Future.traverse(view.authorisations)(detailed)
    } yield {
      PropertyLinkResponse(view.resultCount, detailedLinks)
    }
  }

  private def detailed(authorisation: PropertiesView)(implicit hc: HeaderCarrier, cache: GroupCache): Future[PropertyLink] = {
    for {
      apiPartiesWithGroupAccounts <- withGroupAccounts(authorisation)
      parties = apiPartiesWithGroupAccounts.flatMap { case (p: APIParty, g: GroupAccount) => Party.fromAPIParty(p, g) }
    } yield PropertyLink.fromAPIAuthorisation(authorisation, parties)
  }

  private def withGroupAccounts(authorisation: PropertiesView)(implicit hc: HeaderCarrier, cache: GroupCache): Future[Seq[(APIParty, GroupAccount)]] = {
    Future.traverse(authorisation.parties) { party =>
      cache(groupAccountsConnector.get)(party.authorisedPartyOrganisationId).map(_.map(groupAccount => (party, groupAccount)))
    }.map(_.flatten)
  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long) = authenticated { implicit request =>
    propertyLinksConnector.get(authorisationId) flatMap {
      case Some(authorisation) if authorisedFor(authorisation, clientOrgId, agentOrgId) => toClientProperty(authorisation) map { p => Ok(Json.toJson(p)) }
      case _ => NotFound
    }
  }

  private def authorisedFor(authorisation: PropertiesView, clientOrgId: Long, agentOrgId: Long) = {
    authorisation.authorisationOwnerOrganisationId == clientOrgId && authorisation.parties.exists(_.authorisedPartyOrganisationId == agentOrgId)
  }

  private def toClientProperty(authorisation: PropertiesView)(implicit hc: HeaderCarrier): Future[ClientProperty] = {
    groupAccountsConnector.get(authorisation.authorisationOwnerOrganisationId) map { acc =>
      ClientProperty.build(authorisation, acc)
    }
  }

  def assessments(authorisationId: Long) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    propertyLinksConnector.getAssessment(authorisationId) flatMap {
      case Some(assessment) => detailed(assessment) map { x => Ok(Json.toJson(x)) }
      case None => NotFound
    }
  }
}

