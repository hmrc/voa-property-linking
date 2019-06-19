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

package controllers

import javax.inject.Inject
import auditing.AuditingService
import auth.Authenticated
import binders.GetPropertyLinksParameters
import connectors.auth.DefaultAuthConnector
import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import models._
import models.searchApi.AgentAuthResultFE
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import play.api.mvc.AnyContent
import services.PropertyLinkingService
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import play.api.mvc.{Action, AnyContent}
import utils.Cats

import scala.collection.mutable
import scala.concurrent.Future

case class Memoize[K, V]() {
  private val cache = mutable.Map.empty[K, V]

  def apply(f: K => V): K => V = in => cache.getOrElseUpdate(in, f(in))
}

class PropertyLinkingController @Inject()(
                                           val authConnector: DefaultAuthConnector,
                                           propertyLinksConnector: PropertyLinkingConnector,
                                           service: PropertyLinkingService,
                                           groupAccountsConnector: GroupAccountConnector,
                                           auditingService: AuditingService,
                                           representationsConnector: PropertyRepresentationConnector)
  extends PropertyLinkingBaseController with Authenticated with Cats {

  type GroupCache = Memoize[Long, Future[Option[GroupAccount]]]

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      service.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
        .map { _ =>
          Logger.info(s"create property link: submissionId ${linkRequest.submissionId}")
          auditingService.sendEvent("create property link", linkRequest)
          Created
        }
        .recover { case _: Upstream5xxResponse =>
          Logger.info(s"create property link failure: submissionId ${linkRequest.submissionId}")
          auditingService.sendEvent("create property link failure", linkRequest)
          InternalServerError
        }
    }
  }

  def getMyPropertyLink(submissionId: String, owner: Boolean) = authenticated { implicit request =>
    if(owner)
      service.getMyOrganisationsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
    else
      service.getClientsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
  }

  def getMyPropertyLinks(searchParams: GetPropertyLinksParameters,
                         organisationId: Long,
                         owner: Boolean,
                         paginationParams: Option[PaginationParams]) = authenticated { implicit request =>

    if(owner)
      service.getMyOrganisationsPropertyLinks(searchParams, paginationParams) flatMap {
        case Some(authorisation) => authorisation map {d => Ok(Json.toJson(d))}
        case None => NotFound
      }
    else
      service.getClientsPropertyLinks(searchParams, paginationParams) flatMap {
        case Some(authorisation) => authorisation map {d => Ok(Json.toJson(d))}
        case None => NotFound
      }
  }

  def appointableToAgent(ownerId: Long,
                         agentCode: Long,
                         checkPermission: Option[String],
                         challengePermission: Option[String],
                         paginationParams: PaginationParams,
                         sortfield: Option[String],
                         sortorder: Option[String],
                         address: Option[String],
                         agent: Option[String]) = authenticated { implicit request =>

    groupAccountsConnector.withAgentCode(agentCode.toString) flatMap {
      case Some(agentGroup) => propertyLinksConnector.appointableToAgent(
        ownerId = ownerId,
        agentId = agentGroup.id,
        checkPermission = checkPermission,
        challengePermission = challengePermission,
        params = paginationParams,
        sortfield = sortfield,
        sortorder = sortorder,
        address = address,
        agent = agent).map(x => Ok(Json.toJson(x)))
      case None =>
        Logger.error(s"Agent details lookup failed for agentCode: $agentCode")
        NotFound
    }
  }

  def forAgentSearchAndSort(organisationId: Long,
                            paginationParams: PaginationParams,
                            sortfield: Option[String],
                            sortorder: Option[String],
                            status: Option[String],
                            address: Option[String],
                            baref: Option[String],
                            client: Option[String],
                            representationStatus: Option[String]): Action[AnyContent] = authenticated { implicit request =>
    propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      sortfield = sortfield,
      sortorder = sortorder,
      status = status,
      address = address,
      baref = baref,
      client = client,
      representationStatus = representationStatus
    )
      .map( authResultBE =>
        Ok(Json.toJson(AgentAuthResultFE(authResultBE)))
    )
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

  //Do we did this interface???
//  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long) = authenticated { implicit request =>
//    propertyLinksConnector.get(authorisationId) flatMap {
//      case Some(authorisation) if authorisedFor(authorisation, clientOrgId, agentOrgId) => toClientProperty(authorisation) map { p => Ok(Json.toJson(p)) }
//      case _ => NotFound
//    }
//  }

//  private def authorisedFor(authorisation: PropertiesView, clientOrgId: Long, agentOrgId: Long) = {
//    authorisation.authorisationOwnerOrganisationId == clientOrgId && authorisation.parties.exists(_.authorisedPartyOrganisationId == agentOrgId)
//  }
//
//  private def toClientProperty(authorisation: PropertiesView)(implicit hc: HeaderCarrier): Future[ClientProperty] = {
//    groupAccountsConnector.get(authorisation.authorisationOwnerOrganisationId) map { acc =>
//      ClientProperty.build(authorisation, acc)
//    }
//  }

  def assessments(authorisationId: Long) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    propertyLinksConnector.getAssessment(authorisationId) flatMap {
      case Some(assessment) => detailed(assessment) map { x => Ok(Json.toJson(x)) }
      case None => NotFound
    }
  }

}

