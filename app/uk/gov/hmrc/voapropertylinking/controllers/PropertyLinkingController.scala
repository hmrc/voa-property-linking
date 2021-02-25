/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import models._
import models.mdtp.propertylink.projections.OwnerAuthResult
import models.mdtp.propertylink.requests.{APIPropertyLinkRequest, PropertyLinkRequest}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.Upstream5xxResponse
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.temp.GetMyOrganisationsPropertyLinksParametersWithAgentFiltering
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.connectors.modernised._
import uk.gov.hmrc.voapropertylinking.errorhandler.models.ErrorResponse
import uk.gov.hmrc.voapropertylinking.services.{AssessmentService, PropertyLinkingService}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      authorisationSearchApi: AuthorisationSearchApi,
      propertyLinkService: PropertyLinkingService,
      assessmentService: AssessmentService,
      auditingService: AuditingService
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) with Cats {

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { propertyLinkRequest =>
      propertyLinkService
        .create(APIPropertyLinkRequest.fromPropertyLinkRequest(propertyLinkRequest))
        .map { _ =>
          Logger.info(s"create property link: submissionId ${propertyLinkRequest.submissionId}")
          auditingService.sendEvent("create property link", propertyLinkRequest)
          Accepted
        }
        .recover {
          case _: Upstream5xxResponse =>
            Logger.info(s"create property link failure: submissionId ${propertyLinkRequest.submissionId}")
            auditingService.sendEvent("create property link failure", propertyLinkRequest)
            InternalServerError
        }
    }
  }

  def createOnClientBehalf(clientId: Long): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { propertyLinkRequest =>
      propertyLinkService
        .createOnClientBehalf(APIPropertyLinkRequest.fromPropertyLinkRequest(propertyLinkRequest), clientId)
        .map { _ =>
          Logger.info(s"create property link on client behalf: submissionId ${propertyLinkRequest.submissionId}")
          auditingService.sendEvent("create property link on client behalf", propertyLinkRequest)
          Accepted
        }
        .recover {
          case _: Upstream5xxResponse =>
            Logger.info(
              s"create property link on client behalf failure: submissionId ${propertyLinkRequest.submissionId}")
            auditingService.sendEvent("create property link on client behalf failure", propertyLinkRequest)
            InternalServerError
        }
    }
  }

  def getMyOrganisationsPropertyLink(submissionId: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      propertyLinkService
        .getMyOrganisationsPropertyLink(submissionId)
        .fold(NotFound("my organisation property link not found")) { authorisation =>
          Ok(Json.toJson(authorisation))
        }
  }

  def getMyOrganisationsPropertyLinksCount: Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getMyOrganisationsPropertyLinksCount()
      .map(propertyLinksCount => Ok(Json.toJson(propertyLinksCount)))
  }

  def getMyAgentPropertyLinks(
        agentCode: Long,
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: PaginationParams
  ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getMyAgentPropertyLinks(agentCode, searchParams, paginationParams)
      .map(propertyLinks => Ok(Json.toJson(propertyLinks)))

  }

  def getMyOrganisationsPropertyLinks(
        searchParams: GetMyOrganisationPropertyLinksParameters,
        paginationParams: Option[PaginationParams]
  ): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getMyOrganisationsPropertyLinks(searchParams, paginationParams)
      .map(propertyLinks => Ok(Json.toJson(propertyLinks)))
  }

  def getClientsPropertyLinks(
        searchParams: GetMyClientsPropertyLinkParameters,
        paginationParams: Option[PaginationParams]): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getClientsPropertyLinks(searchParams, paginationParams)
      .fold(NotFound("clients property links not found"))(propertyLinks => Ok(Json.toJson(propertyLinks)))
  }

  def getClientPropertyLinks(
        clientId: Long,
        searchParams: GetClientPropertyLinksParameters,
        paginationParams: Option[PaginationParams]): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getClientPropertyLinks(clientId, searchParams, paginationParams)
      .fold(NotFound("clients property links not found"))(propertyLinks => Ok(Json.toJson(propertyLinks)))
  }

  def getClientsPropertyLink(submissionId: String, projection: String = "propertiesView"): Action[AnyContent] =
    authenticated.async { implicit request =>
      projection match {
        case "propertiesView" =>
          propertyLinkService
            .getClientsPropertyLink(submissionId)
            .fold(NotFound("client property link not found")) { pl =>
              Ok(Json.toJson(PropertiesView(pl.authorisation, Nil)))
            }
        case "clientsPropertyLink" =>
          propertyLinkService
            .getClientsPropertyLink(submissionId)
            .fold(NotFound("client property link not found")) { pl =>
              Ok(Json.toJson(pl.authorisation))
            }
        case _ =>
          Future successful ErrorResponse.notImplementedJsonResult(s"Projection $projection is not implemented")
      }

    }

  def getMyClients(
        clientsParameters: Option[GetClientsParameters],
        paginationParams: Option[PaginationParams]): Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService
      .getMyClients(clientsParameters.getOrElse(GetClientsParameters()), paginationParams)
      .map(clients => Ok(Json.toJson(clients)))
  }

  // $COVERAGE-OFF$
  /*
  TODO Remove this method once external endpoints have caught up.
   */
  def getMyOrganisationPropertyLinksWithAppointable(
        searchParams: GetMyOrganisationsPropertyLinksParametersWithAgentFiltering,
        paginationParams: Option[PaginationParams]
  ): Action[AnyContent] = authenticated.async { implicit request =>
    searchParams.agentAppointed.getOrElse("BOTH") match {
      case "NO" =>
        authorisationSearchApi
          .searchAndSort(
            searchParams.organisationId,
            paginationParams.getOrElse(DefaultPaginationParams),
            searchParams.sortField,
            searchParams.sortOrder,
            searchParams.status,
            searchParams.address,
            searchParams.baref,
            searchParams.agent,
            searchParams.agentAppointed
          )
          .map { response =>
            Ok(Json.toJson(OwnerAuthResult(response)))
          }
      case _ =>
        authorisationSearchApi
          .appointableToAgent(
            searchParams.organisationId,
            searchParams.agentOrganisationId,
            paginationParams.getOrElse(DefaultPaginationParams),
            searchParams.sortField,
            searchParams.sortOrder,
            searchParams.address,
            searchParams.agent
          )
          .map { response =>
            Ok(Json.toJson(OwnerAuthResult(response)))
          }
    }
  }

  // $COVERAGE-ON$

  def getMyOrganisationsAssessments(submissionId: String): Action[AnyContent] = authenticated.async {
    implicit request =>
      assessmentService
        .getMyOrganisationsAssessments(submissionId)
        .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessments =>
          Ok(Json.toJson(propertyLinkWithAssessments)))
  }

  def getClientsAssessments(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    assessmentService
      .getClientsAssessments(submissionId)
      .fold(Ok(Json.toJson(submissionId)))(propertyLinkWithAssessments => Ok(Json.toJson(propertyLinkWithAssessments)))
  }

  def getMyOrganisationsAgents: Action[AnyContent] = authenticated.async { implicit request =>
    propertyLinkService.getMyOrganisationsAgents().map(agentsList => Ok(Json.toJson(agentsList)))
  }

}
