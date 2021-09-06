/*
 * Copyright 2021 HM Revenue & Customs
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

case class IndividualDetails(
      firstName: String,
      lastName: String,
      email: String,
      phone1: String,
      phone2: Option[String],
      addressId: Long)

object IndividualDetails {
  implicit val formats = Json.format[IndividualDetails]
}

case class IndividualAccount(
      externalId: String,
      trustId: Option[String],
      organisationId: Long,
      individualId: Long,
      details: IndividualDetails)

object IndividualAccount {
  implicit val formats = Json.format[IndividualAccount]
}

case class IndividualAccountSubmission(
      externalId: String,
      trustId: Option[String],
      organisationId: Long,
      details: IndividualDetails) {

  def toAPIIndividualAccount =
    APIIndividualAccount(
      PersonData(
        identifyVerificationId = trustId,
        firstName = details.firstName,
        lastName = details.lastName,
        organisationId = organisationId,
        addressUnitId = details.addressId,
        telephoneNumber = details.phone1,
        mobileNumber = details.phone2,
        emailAddress = details.email,
        governmentGatewayExternalId = externalId,
        effectiveFrom = Instant.now
      ))
}

object IndividualAccountSubmission {
  implicit val formats = Json.format[IndividualAccountSubmission]
}

//used as part of POST /organisation - no organisationId is passed here.
case class IndividualAccountSubmissionForOrganisation(
      externalId: String,
      trustId: Option[String],
      details: IndividualDetails)

object IndividualAccountSubmissionForOrganisation {
  implicit val formats = Json.format[IndividualAccountSubmissionForOrganisation]
}
