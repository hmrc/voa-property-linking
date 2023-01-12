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

package uk.gov.hmrc.voapropertylinking.binders.propertylinks.temp

import binders.{Params, ValidationResult}
import uk.gov.hmrc.voapropertylinking.binders.validation.ValidatingBinder

case class GetMyOrganisationsPropertyLinksParametersWithAgentFiltering(
      address: Option[String],
      baref: Option[String],
      agent: Option[String],
      client: Option[String],
      status: Option[String],
      sortField: Option[String],
      sortOrder: Option[String],
      agentAppointed: Option[String],
      organisationId: Long,
      agentOrganisationId: Long
)

object GetMyOrganisationsPropertyLinksParametersWithAgentFiltering
    extends ValidatingBinder[GetMyOrganisationsPropertyLinksParametersWithAgentFiltering] {

  override def validate(params: Params): ValidationResult[GetMyOrganisationsPropertyLinksParametersWithAgentFiltering] =
    (
      readStringOption("address", params),
      readStringOption("baref", params),
      readStringOption("agent", params),
      readStringOption("client", params),
      readStringOption("status", params),
      readStringOption("sortfield", params),
      readStringOption("sortorder", params),
      readStringOption("agentAppointed", params),
      validateLongId("organisationId", params),
      validateLongId("agentOrganisationId", params)
    ).mapN(GetMyOrganisationsPropertyLinksParametersWithAgentFiltering.apply)

  private def readStringOption(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

  private def validateLongId(implicit key: String, params: Params): ValidationResult[Long] =
    read(key, params) andThen asLong

}
