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

import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import models._
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}

import scala.collection.mutable
import scala.concurrent.Future

case class Memoize[K, V]() {
  private val cache = mutable.Map.empty[K, V]
  def apply(f: K => V): K => V = in => cache.getOrElseUpdate(in, f(in))
}

class PropertyLinkingController @Inject()(propertyLinksConnector: PropertyLinkingConnector,
                                          groupAccountsConnector: GroupAccountConnector,
                                          representationsConnector: PropertyRepresentationConnector
                                         ) extends PropertyLinkingBaseController {
  type GroupCache = Memoize[Long, Future[Option[GroupAccount]]]

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      propertyLinksConnector.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
        .map { _ => Created }
        .recover { case _: Upstream5xxResponse => InternalServerError }
    }
  }

  def get(authorisationId: Long) = Action.async { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    propertyLinksConnector.get(authorisationId) flatMap {
      case Some(authorisation) => detailed(authorisation) map { d => Ok(Json.toJson(d)) }
      case None => NotFound
    }
  }

  def find(organisationId: Long, paginationParams: PaginationParams) = Action.async { implicit request =>
    getProperties(organisationId, paginationParams).map(x => Ok(Json.toJson(x)))
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

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long) = Action.async { implicit request =>
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

  def assessments(authorisationId: Long) = Action.async { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    propertyLinksConnector.getAssessment(authorisationId) flatMap {
      case Some(assessment) => detailed(assessment) map { x => Ok(Json.toJson(x)) }
      case None => NotFound
    }
  }
}

