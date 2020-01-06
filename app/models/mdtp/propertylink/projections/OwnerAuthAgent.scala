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

import models.searchApi.{ OwnerAuthAgent => ModernisedOwnerAuthAgent }
import models.AgentPermission.AgentPermission
import play.api.libs.json.Json

case class OwnerAuthAgent(
                           authorisedPartyId: Long,
                           organisationId: Long,
                           organisationName: String,
                           status: String,
                           checkPermission: AgentPermission,
                           challengePermission: AgentPermission,
                           agentCode: Long
                         )

object OwnerAuthAgent {
  implicit val format = Json.format[OwnerAuthAgent]

  def apply(agent: ModernisedOwnerAuthAgent): OwnerAuthAgent = {
    OwnerAuthAgent(
      agent.authorisedPartyId,
      agent.organisationId,
      agent.organisationName,
      agent.status,
      agent.checkPermission,
      agent.challengePermission,
      agent.representativeCode
    )
  }
}

