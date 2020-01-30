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

package models.modernised.externalpropertylink.myorganisations

import models.AgentPermission.AgentPermission
import play.api.libs.json.{Json, OFormat}

case class AgentDetails(
      authorisedPartyId: Long,
      organisationId: Long,
      organisationName: String,
      status: String,
      representationSubmissionId: String,
      representativeCode: Long,
      checkPermission: AgentPermission,
      challengePermission: AgentPermission)

object AgentDetails {
  implicit val format: OFormat[AgentDetails] = Json.format
}
