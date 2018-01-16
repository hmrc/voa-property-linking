/*
 * Copyright 2018 HM Revenue & Customs
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

package models.messages

import java.time.LocalDateTime

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class Message(id: String,
                   recipientOrgId: Long,
                   templateName: String,
                   clientOrgId: Option[Long],
                   clientName: Option[String],
                   agentOrgId: Option[Long],
                   agentName: Option[String],
                   caseReference: String,
                   submissionId: String,
                   timestamp: LocalDateTime,
                   address: String,
                   effectiveDate: LocalDateTime,
                   subject: String,
                   lastRead: Option[LocalDateTime],
                   messageType: String)

object Message {
  val writes: Writes[Message] = Json.writes[Message]
  val reads: Reads[Message] = (
    (__ \ "objectID").read[String] and
      (__ \ "recipientOrganisationID").read[Long] and
      (__ \ "templateName").read[String] and
      (__ \ "clientOrganisationID").readNullable[Long] and
      (__ \ "clientOrganisationName").readNullable[String] and
      (__ \ "agentOrganisationID").readNullable[Long] and
      (__ \ "agentOrganisationName").readNullable[String] and
      (__ \ "businessKey1").read[String] and
      (__ \ "businessKey2").read[String] and
      (__ \ "dateTimeStamp").read[LocalDateTime] and
      (__ \ "address").read[String] and
      (__ \ "effectiveDate").read[LocalDateTime] and
      (__ \ "subject").read[String] and
      (__ \ "lastReadAt").readNullable[LocalDateTime] and
      (__ \ "messageType").read[String]
    )(Message.apply _)

  implicit val format: Format[Message] = Format(reads, writes)
}
