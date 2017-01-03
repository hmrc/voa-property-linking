/*
 * Copyright 2017 HM Revenue & Customs
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

import org.joda.time.LocalDate
import play.api.libs.json.Json

case class APIDetailedIndividualAccount(id: Int, governmentGatewayExternalId: String, personLatestDetail: APIIndividualDetails,
                                        organisationId: Int, organisationLatestDetail: GroupDetails) {

  def toIndividualAccount(address: SimpleAddress) = {
    IndividualAccount(governmentGatewayExternalId, personLatestDetail.identityVerificationId, organisationId, id,
      IndividualDetails(personLatestDetail.firstName, personLatestDetail.lastName, personLatestDetail.emailAddress,
        personLatestDetail.phoneNumber, personLatestDetail.mobileNumber, address)
    )
  }
}

case class APIIndividualDetails(addressUnitId: Int, firstName: String, lastName: String, emailAddress: String, phoneNumber: String,
                             mobileNumber: Option[String], identityVerificationId: String, effectiveFrom: LocalDate)

object APIIndividualDetails {
  implicit val format = Json.format[APIIndividualDetails]
}

object APIDetailedIndividualAccount {
  implicit val format = Json.format[APIDetailedIndividualAccount]
}
