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

case class APIDetailedGroupAccount(id: Int, governmentGatewayGroupId: String, representativeCode: Long, organisationLatestDetail: GroupDetails,
                                   persons: Seq[IndividualSummary]) {

  def toGroupAccount(address: SimpleAddress) = {
    GroupAccount(id, governmentGatewayGroupId, organisationLatestDetail.organisationName, address,
      organisationLatestDetail.organisationEmailAddress, organisationLatestDetail.organisationTelephoneNumber,
      organisationLatestDetail.smallBusinessFlag, organisationLatestDetail.representativeFlag, representativeCode)
  }
}

case class IndividualSummary(personLatestDetail: APIIndividualDetails)

case class GroupDetails(addressUnitId: Int, representativeFlag: Boolean, smallBusinessFlag: Boolean, organisationName: String,
                        organisationEmailAddress: String, organisationTelephoneNumber: String, effectiveFrom: LocalDate)

object GroupDetails {
  implicit val format = Json.format[GroupDetails]
}

object IndividualSummary {
  implicit val format = Json.format[IndividualSummary]
}

object APIDetailedGroupAccount {
  implicit val format = Json.format[APIDetailedGroupAccount]
}
