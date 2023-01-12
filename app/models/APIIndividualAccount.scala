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

import java.time.Instant

import play.api.libs.json.Json

case class PersonData(
      identifyVerificationId: Option[String],
      firstName: String,
      lastName: String,
      organisationId: Long,
      addressUnitId: Long,
      telephoneNumber: String,
      mobileNumber: Option[String],
      emailAddress: String,
      governmentGatewayExternalId: String,
      effectiveFrom: Instant
)

object PersonData {
  implicit val format = Json.format[PersonData]
}

case class APIIndividualAccount(personData: PersonData)

object APIIndividualAccount {
  implicit val format = Json.format[APIIndividualAccount]
}

case class APIIndividualAccountForOrganisation(
      identifyVerificationId: Option[String],
      firstName: String,
      lastName: String,
      addressUnitId: Long,
      telephoneNumber: String,
      mobileNumber: Option[String],
      emailAddress: String,
      governmentGatewayExternalId: String,
      effectiveFrom: Instant)

object APIIndividualAccountForOrganisation {
  implicit val format = Json.format[APIIndividualAccountForOrganisation]
}
