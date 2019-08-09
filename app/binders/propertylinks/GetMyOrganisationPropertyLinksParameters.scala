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

import binders.validation.ValidationUtils
import binders.{Params, ValidationResult}
import play.api.mvc.QueryStringBindable
import utils.Cats
import utils.QueryParamUtils.toQueryString

case class GetMyOrganisationPropertyLinksParameters(
                                       address: Option[String] = None,
                                       baref: Option[String] = None,
                                       agent: Option[String] = None,
                                       client: Option[String] = None,
                                       status: Option[String] = None,
                                       sortField: Option[String] = None,
                                       sortOrder: Option[String] = None
                                     )

object GetMyOrganisationPropertyLinksParameters extends ValidationUtils {

  implicit object Binder extends QueryStringBindable[GetMyOrganisationPropertyLinksParameters] with Cats {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GetMyOrganisationPropertyLinksParameters]] =

      Some(validate(params).leftMap(_.map(_.show).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: GetMyOrganisationPropertyLinksParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[GetMyOrganisationPropertyLinksParameters] =
      (
        validateString("address", params),
        validateString("baref", params),
        validateString("agent", params),
        validateString("client", params),
        validateString("status", params),
        validateString("sortField", params),
        validateString("sortOrder", params))
        .mapN(GetMyOrganisationPropertyLinksParameters.apply)

    def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
      readOption(key, params)
  }
}
