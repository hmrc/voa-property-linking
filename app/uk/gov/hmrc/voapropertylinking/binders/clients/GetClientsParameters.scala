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

package uk.gov.hmrc.voapropertylinking.binders.clients

import java.time.LocalDate

import binders.{Params, ValidationResult}
import uk.gov.hmrc.voapropertylinking.binders.validation.ValidatingBinder

case class GetClientsParameters(
      name: Option[String] = None,
      appointedFromDate: Option[LocalDate] = None,
      appointedToDate: Option[LocalDate] = None
)

object GetClientsParameters extends ValidatingBinder[GetClientsParameters] {

  override def validate(params: Params): ValidationResult[GetClientsParameters] =
    (
      validateString("name", params),
      validateDate("appointedFromDate", params),
      validateDate("appointedToDate", params)
    ).mapN[GetClientsParameters](GetClientsParameters.apply)

  private def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

  def validateDate(implicit key: String, params: Params): ValidationResult[Option[LocalDate]] =
    readOption ifPresent asLocalDate
}
