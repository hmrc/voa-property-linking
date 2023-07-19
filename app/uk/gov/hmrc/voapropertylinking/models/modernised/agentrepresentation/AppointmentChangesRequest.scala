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

package uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AppointmentAction.AppointmentAction
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.AppointmentScope.AppointmentScope

case class AppointmentChangesRequest(
      agentRepresentativeCode: Long,
      action: AppointmentAction,
      scope: AppointmentScope,
      propertyLinks: Option[List[String]],
      listYears: Option[List[String]]
)

object AppointmentChangesRequest {
  implicit val format: OFormat[AppointmentChangesRequest] = Json.format

  def apply(appointAgent: AssignAgent): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = appointAgent.agentRepresentativeCode,
      action = AppointmentAction.APPOINT,
      scope = AppointmentScope.withName(appointAgent.scope),
      propertyLinks = None,
      listYears = None
    )

  def apply(appointAgent: AssignAgentToSomeProperties): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = appointAgent.agentCode,
      action = AppointmentAction.APPOINT,
      scope = AppointmentScope.PROPERTY_LIST,
      propertyLinks = Some(appointAgent.propertyLinkIds),
      listYears = None
    )

  def apply(unassignAgent: UnassignAgent): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = unassignAgent.agentRepresentativeCode,
      action = AppointmentAction.REVOKE,
      scope = AppointmentScope.withName(unassignAgent.scope),
      propertyLinks = None,
      listYears = None
    )

  def apply(unassignAgent: UnassignAgentFromSomeProperties): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = unassignAgent.agentCode,
      action = AppointmentAction.REVOKE,
      scope = AppointmentScope.PROPERTY_LIST,
      propertyLinks = Some(unassignAgent.propertyLinkIds),
      listYears = None
    )

  def apply(removeAgent: RemoveAgentFromIpOrganisation): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = removeAgent.agentRepresentativeCode,
      action = AppointmentAction.REVOKE,
      scope = AppointmentScope.RELATIONSHIP,
      propertyLinks = None,
      listYears = None
    )
  //TODO: remove all apply methods/hard coding of action/scope & just proxy the AppointmentChangesRequest through to backend.
  def apply(changeRequest: AppointmentChangesRequest): AppointmentChangesRequest =
    AppointmentChangesRequest(
      agentRepresentativeCode = changeRequest.agentRepresentativeCode,
      action = changeRequest.action,
      scope = changeRequest.scope,
      propertyLinks = changeRequest.propertyLinks,
      listYears = changeRequest.listYears
    )
}
