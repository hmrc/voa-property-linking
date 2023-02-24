/*
 * Copyright 2023 HM Revenue & Customs
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

import models.modernised.ValuationHistory
import play.api.libs.json.{Json, OFormat}

case class APIValuationHistory(
      asstRef: Long,
      listYear: String,
      uarn: Long,
      effectiveDate: Option[LocalDate],
      rateableValue: Option[Long],
      address: String,
      billingAuthorityReference: String,
      currentFromDate: Option[LocalDate],
      currentToDate: Option[LocalDate]
) {

  def capatalise: APIValuationHistory = this.copy(address = address.toUpperCase)
}

object APIValuationHistory {

  implicit val formats: OFormat[APIValuationHistory] = Json.format

  def apply(history: ValuationHistory): APIValuationHistory =
    APIValuationHistory(
      asstRef = history.asstRef,
      listYear = history.listYear,
      uarn = history.uarn,
      effectiveDate = history.effectiveDate,
      rateableValue = history.rateableValue.map { d =>
        d.longValue
      },
      address = history.address,
      billingAuthorityReference = history.billingAuthorityReference,
      currentFromDate = history.currentFromDate,
      currentToDate = history.currentToDate
    )

}
