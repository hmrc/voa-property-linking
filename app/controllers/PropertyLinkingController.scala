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

import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import binders.propertylinks.temp.GetMyOrganisationsPropertyLinksParametersWithAgentFiltering
import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import javax.inject.{Inject, Named}
import models._
import models.mdtp.propertylink.requests.{APIPropertyLinkRequest, PropertyLinkRequest}
import models.modernised.mdtpdashboard.LegacyPropertiesView
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.{AssessmentService, PropertyLinkingService}
import uk.gov.hmrc.http.{HeaderCarrier, Upstream5xxResponse}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{AuthorisationManagementApi, AuthorisationSearchApi, CustomerManagementApi, MdtpDashboardManagementApi}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

case class Memoize[K, V]() {
  private val cache = mutable.Map.empty[K, V]

  def apply(f: K => V): K => V = in => cache.getOrElseUpdate(in, f(in))
}

class PropertyLinkingController @Inject()(
                                           authenticated: AuthenticatedActionBuilder,
                                           authorisationSearchApi: AuthorisationSearchApi,
                                           mdtpDashboardManagementApi: MdtpDashboardManagementApi,
                                           propertyLinkService: PropertyLinkingService,
                                           assessmentService: AssessmentService,
                                           customerManagementApi: CustomerManagementApi,
                                           auditingService: AuditingService,
                                           authorisationManagementApi: AuthorisationManagementApi,
                                           @Named("agentQueryParameterEnabledExteranl") agentQueryParameterEnabledExteranl: Boolean
                                         )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController with Cats {

  type GroupCache = Memoize[Long, Future[Option[GroupAccount]]]

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { propertyLinkRequest =>
      propertyLinkService
        .create(APIPropertyLinkRequest.fromPropertyLinkRequest(propertyLinkRequest))
        .map { _ =>
          Logger.info(s"create property link: submissionId ${propertyLinkRequest.submissionId}")
          auditingService.sendEvent("create property link", propertyLinkRequest)
          Accepted
        }.recover {
        case _: Upstream5xxResponse =>
          Logger.info(s"create property link failure: submissionId ${propertyLinkRequest.submissionId}")
          auditingService.sendEvent("create property link failure", propertyLinkRequest)
          InternalServerError
        }
    }
  }

  def getMyOrganisationsPropertyLink(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getMyOrganisationsPropertyLink(submissionId)
      .fold(NotFound("my organisation property link not found")) {authorisation => Ok(Json.toJson(authorisation))}
  }

  def getMyOrganisationsPropertyLinks(
                                       searchParams: GetMyOrganisationPropertyLinksParameters,
                                       paginationParams: Option[PaginationParams],
                                       organisationId: Option[Long]
                                     ): Action[AnyContent] = authenticated.async { implicit request =>
    //TODO remove once modernised external has caught up.
    if (searchParams.sortField.map(_ == "AGENT").getOrElse(false) && agentQueryParameterEnabledExteranl) {

      organisationId.fold(Future.successful(BadRequest("organisationId is required for this query.")))(
        id =>
          authorisationSearchApi.searchAndSort(
            id,
            paginationParams.getOrElse(DefaultPaginationParams),
            searchParams.sortField,
            searchParams.sortOrder,
            searchParams.status,
            searchParams.address,
            searchParams.baref,
            searchParams.agent
          )
            .map( response => Ok(Json.toJson(response))))
    } else {
      propertyLinkService
        .getMyOrganisationsPropertyLinks(searchParams, paginationParams)
        .fold(NotFound("my organisation property links not found"))(propertyLinks => Ok(Json.toJson(propertyLinks)))
    }
  }

  def getClientsPropertyLinks(
                               searchParams: GetMyClientsPropertyLinkParameters,
                               paginationParams: Option[PaginationParams]): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getClientsPropertyLinks(searchParams, paginationParams)
      .fold(NotFound("clients property links not found"))(propertyLinks => Ok(Json.toJson(propertyLinks)))
  }

  def getClientsPropertyLink(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getClientsPropertyLink(submissionId)
      .fold(NotFound("client property link not found")){authorisation => Ok(Json.toJson(authorisation))}
  }

  /*
  To Remove this method once external endpoints have catched up.
   */
  def getMyOrganisationPropertyLinksWithAppointable(
                                                     searchParams: GetMyOrganisationsPropertyLinksParametersWithAgentFiltering,
                                                     paginationParams: Option[PaginationParams]
                                                   ): Action[AnyContent] = authenticated.async { implicit request =>

    searchParams.agentAppointed.getOrElse("BOTH") match {
      case "NO" =>
        authorisationSearchApi.searchAndSort(
          searchParams.organisationId,
          paginationParams.getOrElse(DefaultPaginationParams),
          searchParams.sortField,
          searchParams.sortOrder,
          searchParams.status,
          searchParams.address,
          searchParams.baref,
          searchParams.agent,
          searchParams.agentAppointed
        ).map { response =>
          Ok(Json.toJson(response))
        }
      case _    =>
        authorisationSearchApi.appointableToAgent(
          searchParams.organisationId,
          searchParams.agentOrganisationId,
          searchParams.checkPermission,
          searchParams.challengePermission,
          paginationParams.getOrElse(DefaultPaginationParams),
          searchParams.sortField,
          searchParams.sortOrder,
          searchParams.address,
          searchParams.agent
        ).map { response =>
          Ok(Json.toJson(response))
        }
    }
  }

  def getMyOrganisationsAssessments(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    assessmentService
      .getMyOrganisationsAssessments(submissionId)
      .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessments => Ok(Json.toJson(propertyLinkWithAssessments)))
  }

  def getClientsAssessments(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    assessmentService
      .getClientsAssessments(submissionId)
      .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessments => Ok(Json.toJson(propertyLinkWithAssessments)))
  }

  def getMyOrganisationsAssessmentsWithCapacity(submissionId: String, authorisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    assessmentService
      .getMyOrganisationsAssessments(submissionId)
      .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessmentsAndCapacity => Ok(Json.toJson(propertyLinkWithAssessmentsAndCapacity)))
  }

  def getClientsAssessmentsWithCapacity(submissionId: String, authorisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    assessmentService
      .getClientsAssessments(submissionId)
      .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessmentsAndCapacity => Ok(Json.toJson(propertyLinkWithAssessmentsAndCapacity)))
  }

  def clientProperty(authorisationId: Long, clientOrgId: Long, agentOrgId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    mdtpDashboardManagementApi
      .get(authorisationId)
      .flatMap {
        case Some(authorisation) if authorisedFor(authorisation, clientOrgId, agentOrgId) => toClientProperty(authorisation) map { p => Ok(Json.toJson(p)) }
        case _ => Future.successful(NotFound)
    }
  }

  private def authorisedFor(authorisation: LegacyPropertiesView, clientOrgId: Long, agentOrgId: Long) = {
    authorisation.authorisationOwnerOrganisationId == clientOrgId && authorisation.parties.exists(_.authorisedPartyOrganisationId == agentOrgId)
  }

  private def toClientProperty(authorisation: LegacyPropertiesView)(implicit hc: HeaderCarrier): Future[ClientProperty] = {
    customerManagementApi.getDetailedGroupAccount(authorisation.authorisationOwnerOrganisationId) map { acc =>
      ClientProperty.build(authorisation, acc)
    }
  }
}

