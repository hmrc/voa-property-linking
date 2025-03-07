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

package uk.gov.hmrc.voapropertylinking.binders.propertylinks

import java.time.LocalDate
import binders._
import uk.gov.hmrc.voapropertylinking.binders.validation.ValidatingBinder
case class GetClientPropertyLinksParameters(
      address: Option[String] = None,
      baref: Option[String] = None,
      status: Option[String] = None,
      sortField: Option[String] = None,
      sortOrder: Option[String] = None,
      representationStatus: Option[String] = None,
      appointedFromDate: Option[LocalDate] = None,
      appointedToDate: Option[LocalDate] = None,
      uarn: Option[Long] = None,
      client: Option[String] = None
)

object GetClientPropertyLinksParameters extends ValidatingBinder[GetClientPropertyLinksParameters] {

  override def validate(params: Params): ValidationResult[GetClientPropertyLinksParameters] =
    (
      validateString("address", params),
      validateString("baref", params),
      validateString("status", params),
      validateString("sortField", params),
      validateString("sortOrder", params),
      validateString("representationStatus", params),
      validateLocalDate("appointedFromDate", params),
      validateLocalDate("appointedToDate", params),
      validateLong("uarn", params),
      validateString("client", params)
    ).mapN(GetClientPropertyLinksParameters.apply)

  def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

  def validateLocalDate(implicit key: String, params: Params): ValidationResult[Option[LocalDate]] =
    readOption ifPresent asLocalDate

  def validateLong(implicit key: String, params: Params): ValidationResult[Option[Long]] =
    readOption ifPresent asLong
}
