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

package models

import java.time.LocalDate

import play.api.libs.json.Json

case class Assessment(
                       authorisationId: Long,
                       assessmentRef: Long,
                       listYear: String,
                       uarn: Long,
                       effectiveDate: LocalDate,
                       rateableValue: Option[Long],
                       address: PropertyAddress,
                       billingAuthorityReference: String,
                       capacity: CapacityDeclaration
                     )

object Assessment {
  implicit val formats = Json.format[Assessment]

  def fromAPIValuationHistory(valuationHistory: APIValuationHistory, authorisationId: Long,
                              capacityDeclaration: CapacityDeclaration) = {
    Assessment(
      authorisationId,
      valuationHistory.asstRef,
      valuationHistory.listYear,
      valuationHistory.uarn,
      valuationHistory.effectiveDate,
      valuationHistory.rateableValue,
      PropertyAddress.fromString(valuationHistory.address),
      valuationHistory.billingAuthorityReference,
      capacityDeclaration
    )
  }

}
