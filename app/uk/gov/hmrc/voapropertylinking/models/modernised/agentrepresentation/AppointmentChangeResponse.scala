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

package uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation

import play.api.libs.json._

case class AppointmentChangeResponse(appointmentChangeId: String)

object AppointmentChangeResponse {
  implicit val reads: Reads[AppointmentChangeResponse] =
    (__ \ "agentAppointmentChangeId")
      .read[String]
      .map(agentAppointmentChangeId => AppointmentChangeResponse(agentAppointmentChangeId))

  implicit val writes = Json.writes[AppointmentChangeResponse]

}
