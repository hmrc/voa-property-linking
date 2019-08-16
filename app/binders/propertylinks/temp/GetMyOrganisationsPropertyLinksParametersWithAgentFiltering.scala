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

package binders.propertylinks.temp

import binders.validation.ValidationUtils
import binders.{Params, ValidationResult}
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.voapropertylinking.utils.Cats
import uk.gov.hmrc.voapropertylinking.utils.QueryParamUtils.toQueryString

case class GetMyOrganisationsPropertyLinksParametersWithAgentFiltering(
                                                                        address: Option[String] = None,
                                                                        baref: Option[String] = None,
                                                                        agent: Option[String] = None,
                                                                        client: Option[String] = None,
                                                                        status: Option[String] = None,
                                                                        sortField: Option[String] = None,
                                                                        sortOrder: Option[String] = None,
                                                                        agentAppointed: Option[String] = None,
                                                                        organisationId: Long,
                                                                        agentOrganisationId: Long,
                                                                        checkPermission: Option[String],
                                                                        challengePermission: Option[String]
                                                                 )

object GetMyOrganisationsPropertyLinksParametersWithAgentFiltering extends ValidationUtils {

  implicit object Binder extends QueryStringBindable[GetMyOrganisationsPropertyLinksParametersWithAgentFiltering] with Cats {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, GetMyOrganisationsPropertyLinksParametersWithAgentFiltering]] =

      Some(validate(params).leftMap(_.map(_.show).toList.mkString(", ")).toEither)
    override def unbind(key: String, value: GetMyOrganisationsPropertyLinksParametersWithAgentFiltering): String = toQueryString(value)
    private def validate(params: Params): ValidationResult[GetMyOrganisationsPropertyLinksParametersWithAgentFiltering] =
      (
        validateOptString("address", params),
        validateOptString("baref", params),
        validateOptString("agent", params),
        validateOptString("client", params),
        validateOptString("status", params),
        validateOptString("sortfield", params),
        validateOptString("sortorder", params),
        validateOptString("agentAppointed", params),
        validateOrganisationId("organisationId", params),
        validateAgentCode("agentOrganisationId", params),
        validateOptString("checkPermission", params),
        validateOptString("challengePermission", params)
      ).mapN(GetMyOrganisationsPropertyLinksParametersWithAgentFiltering.apply)


  }

  def validateAgentCode(implicit key: String, params: Params): ValidationResult[Long] =
    read(key, params) andThen asLong

  def validateOrganisationId(implicit key: String, params: Params): ValidationResult[Long] =
    read(key, params) andThen asLong

  def validateOptString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)
}