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

package models.modernised.mdtpdashboard

import java.time.{Instant, LocalDate}

import models.{APIParty, APIValuationHistory}
import play.api.libs.json.{Json, OFormat}

case class LegacyPropertiesView(
                           authorisationId: Long,
                           uarn: Long,
                           authorisationOwnerOrganisationId: Long,
                           authorisationOwnerPersonId: Long,
                           authorisationStatus: String,
                           authorisationMethod: String,
                           authorisationOwnerCapacity: String,
                           startDate: LocalDate,
                           endDate: Option[LocalDate],
                           submissionId: String,
                           NDRListValuationHistoryItems: Seq[APIValuationHistory],
                           parties: Seq[APIParty]) {

  def upperCase = this.copy(NDRListValuationHistoryItems = NDRListValuationHistoryItems.map(_.capatalise))

  def hasValidStatus: Boolean = {
    !Seq("DECLINED", "REVOKED", "MORE_EVIDENCE_REQUIRED").contains(authorisationStatus.toUpperCase)
  }
}

object LegacyPropertiesView {
  implicit val format: OFormat[LegacyPropertiesView] = Json.format
}