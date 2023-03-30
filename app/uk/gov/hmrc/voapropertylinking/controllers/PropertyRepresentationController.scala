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

package uk.gov.hmrc.voapropertylinking.controllers

import models._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.{AuthorisationManagementApi, ExternalOrganisationManagementApi, ExternalPropertyLinkApi}
import uk.gov.hmrc.voapropertylinking.connectors.modernised._
import uk.gov.hmrc.voapropertylinking.errorhandler.models.ErrorResponse
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AppointmentChangeResponse._
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      modernisedAuthorisationManagementApi: ModernisedAuthorisationManagementApi,
      modernisedOrganisationManagementApi: ModernisedExternalOrganisationManagementApi,
      modernisedExternalPropertyLinkApi: ModernisedExternalPropertyLinkApi,
      authorisationManagementApi: AuthorisationManagementApi,
      organisationManagementApi: ExternalOrganisationManagementApi,
      propertyLinkApi: ExternalPropertyLinkApi,
      featureSwitch: FeatureSwitch,
      auditingService: AuditingService
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def validateAgentCode(agentCode: Long, authorisationId: Long): Action[AnyContent] = authenticated.async {
    implicit request =>
      lazy val validateAgentCodeResponse: Future[Either[Long, String]] =
        if (featureSwitch.isBstDownstreamEnabled) {
          authorisationManagementApi.validateAgentCode(agentCode, authorisationId)
        } else {
          modernisedAuthorisationManagementApi.validateAgentCode(agentCode, authorisationId)
        }
      validateAgentCodeResponse
        .map { errorOrOrganisationId =>
          errorOrOrganisationId.fold(
            orgId => Ok(Json.obj("organisationId"    -> orgId)),
            errorString => Ok(Json.obj("failureCode" -> errorString)))
        }
  }

  def response(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[APIRepresentationResponse] { representationResponse =>
      lazy val response: Future[HttpResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          authorisationManagementApi.response(representationResponse)
        } else {
          modernisedAuthorisationManagementApi.response(representationResponse)
        }
      response
        .map { _ =>
          auditingService.sendEvent("agent representation response", representationResponse)
          Ok("")
        }
    }
  }

  def revokeClientProperty(submissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    if (featureSwitch.isBstDownstreamEnabled) {
      propertyLinkApi.revokeClientProperty(submissionId).map(_ => NoContent)
    } else {
      modernisedExternalPropertyLinkApi.revokeClientProperty(submissionId).map(_ => NoContent)
    }
  }

  def getAgentDetails(agentCode: Long): Action[AnyContent] = authenticated.async { implicit request =>
    lazy val getAgentDetails: Future[Option[AgentDetails]] =
      if (featureSwitch.isBstDownstreamEnabled) {
        organisationManagementApi.getAgentDetails(agentCode)
      } else {
        modernisedOrganisationManagementApi.getAgentDetails(agentCode)
      }
    getAgentDetails.map {
      case None        => ErrorResponse.notFoundJsonResult("Agent does not exist")
      case Some(agent) => Ok(Json.toJson(agent))
    }
  }

  def assignAgent(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[AssignAgent] { appointAgent =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(appointAgent))
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(appointAgent))
        }
      agentAppointmentChanges
        .map { response =>
          Accepted(Json.toJson(response))
        }
    }
  }

  def assignAgentToSomeProperties(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[AssignAgentToSomeProperties] { appointAgent =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(appointAgent))
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(appointAgent))
        }
      agentAppointmentChanges
        .map { response =>
          Accepted(Json.toJson(response))
        }
    }
  }

  def unassignAgent(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[UnassignAgent] { unassignAgent =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(unassignAgent))
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(unassignAgent))
        }
      agentAppointmentChanges
        .map { response =>
          Accepted(Json.toJson(response))
        }
    }
  }

  def removeAgentFromOrganisation(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[RemoveAgentFromIpOrganisation] { removeAgent =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(removeAgent))
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(removeAgent))
        }
      agentAppointmentChanges
        .map { response =>
          Accepted(Json.toJson(response))
        }
    }
  }

  def unassignAgentFromSomeProperties(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[UnassignAgentFromSomeProperties] { unassignAgent =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(unassignAgent))
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(AppointmentChangesRequest(unassignAgent))
        }
      agentAppointmentChanges
        .map { response =>
          Accepted(Json.toJson(response))
        }
    }
  }
}
