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

package binders.propertylinks

import binders.validation.ValidatingBinder
import binders.{Params, ValidationResult}

case class GetMyClientsPropertyLinkParameters(
                                               address: Option[String] = None,
                                               baref: Option[String] = None,
                                               client: Option[String] = None,
                                               status: Option[String] = None,
                                               sortField: Option[String] = None,
                                               sortOrder: Option[String] = None,
                                               representationStatus: Option[String] = None
                                             )

object GetMyClientsPropertyLinkParameters extends ValidatingBinder[GetMyClientsPropertyLinkParameters] {

  override def validate(params: Params): ValidationResult[GetMyClientsPropertyLinkParameters] =
    (
      validateString("address", params),
      validateString("baref", params),
      validateString("client", params),
      validateString("status", params),
      validateString("sortField", params),
      validateString("sortOrder", params),
      validateString("representationStatus", params)
      ).mapN(GetMyClientsPropertyLinkParameters.apply)

  def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

}
