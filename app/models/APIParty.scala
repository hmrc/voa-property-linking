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

package models

import models.AgentPermission.AgentPermission
import play.api.libs.json.Json

case class Permissions(
                        id: Long,
                        checkPermission: AgentPermission,
                        challengePermission: AgentPermission,
                        endDate: Option[String]
                      )
object Permissions{
  implicit val format = Json.format[Permissions]
}
case class APIParty(
                     id: Long,
                     authorisedPartyStatus:String,
                     authorisedPartyOrganisationId: Long,
                     permissions: Seq[Permissions]
                   )

object APIParty {
  implicit val format = Json.format[APIParty]
}

case class LegacyParty (
                   authorisedPartyId: Long,
                   agentCode: Long,
                   organisationName: String,
                   organisationId: Long,
                   checkPermission: AgentPermission,
                   challengePermission: AgentPermission
                 )

object LegacyParty {
  implicit val format = Json.format[LegacyParty]
}


