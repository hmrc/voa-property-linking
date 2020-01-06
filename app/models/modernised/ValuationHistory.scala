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

package models.modernised

import java.time.LocalDate

import play.api.libs.json.{Json, OFormat}

case class ValuationHistory(
                             asstRef: Long,
                             listYear: String,
                             uarn: Long,
                             billingAuthorityReference: String,
                             address: String,
                             description: Option[String],
                             specialCategoryCode: Option[String],
                             compositeProperty: Option[String],
                             effectiveDate: Option[LocalDate],
                             listAlterationDate: Option[LocalDate],
                             numberOfPreviousProposals: Option[Int],
                             settlementCode: Option[String],
                             totalAreaM2: Option[BigDecimal],
                             costPerM2: Option[BigDecimal],
                             rateableValue: Option[BigDecimal],
                             transitionalCertificate: Option[Boolean],
                             deletedIndicator: Option[Boolean],
                             valuationDetailsAvailable: Option[Boolean],
                             billingAuthorityCode: Option[String],
                             currentFromDate: Option[LocalDate] = None,
                             currentToDate: Option[LocalDate] = None)


object ValuationHistory {
  implicit val valuationHistoryFormats: OFormat[ValuationHistory] = Json.format
}
