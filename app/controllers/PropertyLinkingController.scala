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

import auditing.AuditingService
import auth.Authenticated
import binders.GetPropertyLinksParameters
import connectors.auth.DefaultAuthConnector
import connectors.{GroupAccountConnector, PropertyRepresentationConnector}
import javax.inject.{Inject, Named}

import models._
import models.mdtp.propertylinking.requests.{APIPropertyLinkRequest, PropertyLinkRequest}
import play.api.Logger
import play.api.libs.json.Json
import services.{AssessmentService, PropertyLinkingService}
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
                                           propertyLinkService: PropertyLinkingService,
                                           assessmentService: AssessmentService,
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

  def getMyOrganisationsPropertyLink(submissionId: String) = authenticated { implicit request =>
    propertyLinkService.getMyOrganisationsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
  }

  def getClientsPropertyLink(submissionId: String) = authenticated { implicit request =>
      propertyLinkService.getClientsPropertyLink(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation => Ok(Json.toJson(authorisation))}
  }

  def getMyOrganisationsPropertyLinks(searchParams: GetPropertyLinksParameters,
                                      paginationParams: Option[PaginationParams]) = authenticated { implicit request =>

    propertyLinkService.getMyOrganisationsPropertyLinks(searchParams, paginationParams).value flatMap {
      response => {
        Ok(Json.toJson(response))
      }
    }
  }

  def getClientsPropertyLinks(searchParams: GetPropertyLinksParameters,
                         paginationParams: Option[PaginationParams]) = authenticated { implicit request =>
    propertyLinkService.getClientsPropertyLinks(searchParams, paginationParams).value flatMap {
      response => {
        Ok(Json.toJson(response))
      }
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

  def getMyOrganisationsAssessments(submissionId: String) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    assessmentService.getMyOrganisationsAssessments(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation =>
      Ok(Json.toJson(authorisation))}
  }

  def getClientsAssessments(submissionId: String) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    assessmentService.getClientsAssessments(submissionId).fold(Ok(Json.toJson(submissionId))) {authorisation =>
      Ok(Json.toJson(authorisation))}
  }

  def getMyOrganisationsAssessmentsWithCapacity(submissionId: String, authorisationId: Long) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()

    assessmentService.getMyOrganisationsAssessmentsWithCapacity(submissionId, authorisationId).fold(Ok(Json.toJson(submissionId))) {assessment: Assessments =>
      Ok(Json.toJson(assessment))}
  }

  def getClientsAssessmentsWithCapacity(submissionId: String, authorisationId: Long) = authenticated { implicit request =>
    implicit val cache = Memoize[Long, Future[Option[GroupAccount]]]()
    assessmentService.getClientsAssessmentsWithCapacity(submissionId, authorisationId).fold(Ok(Json.toJson(submissionId))) {assessment =>
      Ok(Json.toJson(assessment))}
  }

}

