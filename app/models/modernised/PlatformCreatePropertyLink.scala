/*
 * Copyright 2019 HM Revenue & Customs
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

package models.modernised

import java.time.LocalDate

import models.modernised.Capacity.Capacity
import models.modernised.ProvidedEvidence.ProvidedEvidence
import play.api.libs.json.{Json, OFormat}

case class PlatformCreatePropertyLink(
                               uarn: Long,
                               capacity: Capacity,
                               startDate: LocalDate,
                               endDate: Option[LocalDate],
                               providedEvidence: ProvidedEvidence,
                               evidence: Seq[Evidence])

object PlatformCreatePropertyLink {
  implicit val format: OFormat[CreatePropertyLink] = Json.format

  object InvalidEndDate {
    def unapply(x: CreatePropertyLink): Option[(LocalDate, LocalDate)] =
      x.endDate match {
        case None                                          => None
        case Some(endDate) if endDate.isAfter(x.startDate) => None
        case Some(endDate)                                 => Some(x.startDate -> endDate)
      }
  }

}
