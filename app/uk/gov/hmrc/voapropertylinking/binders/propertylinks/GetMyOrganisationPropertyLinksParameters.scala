/*
 * Copyright 2022 HM Revenue & Customs
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

import binders.{Params, ValidationResult}
import uk.gov.hmrc.voapropertylinking.binders.validation.ValidatingBinder

case class GetMyOrganisationPropertyLinksParameters(
      address: Option[String] = None,
      uarn: Option[Long] = None,
      baref: Option[String] = None,
      agent: Option[String] = None,
      client: Option[String] = None,
      status: Option[String] = None,
      sortField: Option[String] = None,
      sortOrder: Option[String] = None
)

object GetMyOrganisationPropertyLinksParameters extends ValidatingBinder[GetMyOrganisationPropertyLinksParameters] {

  override def validate(params: Params): ValidationResult[GetMyOrganisationPropertyLinksParameters] =
    (
      validateString("address", params),
      validateUarn("uarn", params),
      validateString("baref", params),
      validateString("agent", params),
      validateString("client", params),
      validateString("status", params),
      validateString("sortField", params),
      validateString("sortOrder", params))
      .mapN(GetMyOrganisationPropertyLinksParameters.apply)

  private def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

  private def validateUarn(implicit key: String, params: Params): ValidationResult[Option[Long]] =
    readOption.ifPresent(uarnString => asLong(uarnString) andThen min(1))

}
