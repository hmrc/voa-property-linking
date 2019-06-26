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
import play.api.Logger
import play.api.libs.json.Json
import services.PropertyLinkingService
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
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
                                           propertyLinkService: PropertyLinkingService,
                                           groupAccountsConnector: GroupAccountConnector,
                                           auditingService: AuditingService,
                                           representationsConnector: PropertyRepresentationConnector)
  extends PropertyLinkingBaseController with Authenticated with Cats {

  type GroupCache = Memoize[Long, Future[Option[GroupAccount]]]

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      propertyLinkService.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
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
      propertyLinkService.getMyOrganisationsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
    else
      propertyLinkService.getClientsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
  }

  def getMyPropertyLinks(searchParams: GetPropertyLinksParameters,
                         organisationId: Long,
                         owner: Boolean,
                         paginationParams: Option[PaginationParams]) = authenticated { implicit request =>

    if(owner)
      propertyLinkService.getMyOrganisationsPropertyLinks(searchParams, paginationParams) flatMap {
        case Some(authorisation) => authorisation map {d => Ok(Json.toJson(d))}
        case None => NotFound
      }
    else
      propertyLinkService.getClientsPropertyLinks(searchParams, paginationParams) flatMap {
        case Some(authorisation) => authorisation map {d => Ok(Json.toJson(d))}
        case None => NotFound
      }
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

  def assessments(authorisationId: Long) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    propertyLinksConnector.getAssessment(authorisationId) flatMap {
      case Some(assessment) => detailed(assessment) map { x => Ok(Json.toJson(x)) }
      case None => NotFound
    }
  }

}

