/*
 * Copyright 2020 HM Revenue & Customs
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

package models.mdtp.propertylink.projections

import models.searchApi.{OwnerAuthorisation => ModerniedOwnerAuthorisaton}
import models.AgentPermission
import models.modernised.externalpropertylink.myorganisations.SummaryPropertyLinkWithAgents
import play.api.libs.json.Json

case class OwnerAuthorisation(
      authorisationId: Long,
      status: String,
      submissionId: String,
      uarn: Long,
      address: String,
      localAuthorityRef: String,
      agents: Seq[OwnerAuthAgent]
) {

  def capatilise() = this.copy(address = address.toUpperCase)
}

object OwnerAuthorisation {
  implicit val ownerAuthorisation = Json.format[OwnerAuthorisation]

  def apply(authorisation: ModerniedOwnerAuthorisaton): OwnerAuthorisation =
    OwnerAuthorisation(
      authorisation.authorisationId,
      authorisation.status,
      authorisation.submissionId,
      authorisation.uarn,
      authorisation.address,
      authorisation.localAuthorityRef,
      authorisation.agents.map(OwnerAuthAgent.apply)
    )

  def apply(propertyLink: SummaryPropertyLinkWithAgents): OwnerAuthorisation =
    OwnerAuthorisation(
      authorisationId = propertyLink.authorisationId,
      status = propertyLink.status.toString,
      submissionId = propertyLink.submissionId,
      uarn = propertyLink.uarn,
      address = propertyLink.address,
      localAuthorityRef = propertyLink.localAuthorityRef,
      agents = propertyLink.agents.map(
        agent =>
          OwnerAuthAgent(
            authorisedPartyId = agent.authorisedPartyId,
            organisationId = agent.organisationId,
            organisationName = agent.organisationName,
            agentCode = agent.representativeCode
        ))
    )
}
