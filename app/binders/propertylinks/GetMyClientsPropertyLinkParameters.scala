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

case class GetMyClientsPropertyLinkParameters(
                                                     address: Option[String] = None,
                                                     baref: Option[String] = None,
                                                     agent: Option[String] = None,
                                                     status: Option[String] = None,
                                                     sortField: Option[String] = None,
                                                     sortOrder: Option[String] = None,
                                                     representationStatus: Option[String] = None
                                                   )

object GetMyClientsPropertyLinkParameters extends ValidationUtils {

  implicit object Binder extends QueryStringBindable[GetMyClientsPropertyLinkParameters] with Cats {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GetMyClientsPropertyLinkParameters]] =

      Some(validate(params).leftMap(_.map(_.show).toList.mkString(", ")).toEither)

    override def unbind(key: String, value: GetMyClientsPropertyLinkParameters): String = toQueryString(value)

    private def validate(params: Params): ValidationResult[GetMyClientsPropertyLinkParameters] =
      (
        validateString("address", params),
        validateString("baref", params),
        validateString("agent", params),
        validateString("status", params),
        validateString("sortfield", params),
        validateString("sortorder", params),
        validateString("representationStatus", params)
      ).mapN(GetMyClientsPropertyLinkParameters.apply)

    def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
      readOption(key, params)
  }
}
