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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.{ExternalOrganisationManagementApi, ExternalPropertyLinkApi}
import uk.gov.hmrc.voapropertylinking.connectors.modernised._
import uk.gov.hmrc.voapropertylinking.errorhandler.models.ErrorResponse
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AppointmentChangeResponse._
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      modernisedOrganisationManagementApi: ModernisedExternalOrganisationManagementApi,
      modernisedExternalPropertyLinkApi: ModernisedExternalPropertyLinkApi,
      organisationManagementApi: ExternalOrganisationManagementApi,
      propertyLinkApi: ExternalPropertyLinkApi,
      featureSwitch: FeatureSwitch,
      auditingService: AuditingService
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

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

  def submitAppointmentChanges(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[AppointmentChangesRequest] { appointRequest =>
      lazy val agentAppointmentChanges: Future[AppointmentChangeResponse] =
        if (featureSwitch.isBstDownstreamEnabled) {
          organisationManagementApi.agentAppointmentChanges(appointRequest)
        } else {
          modernisedOrganisationManagementApi.agentAppointmentChanges(appointRequest)
        }
      agentAppointmentChanges.map { response =>
        Accepted(Json.toJson(response))
      }
    }
  }
}
