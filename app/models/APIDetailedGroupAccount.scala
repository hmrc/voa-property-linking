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

import play.api.libs.json.{Json, OFormat}

case class APIDetailedGroupAccount(
      id: Long,
      governmentGatewayGroupId: String,
      representativeCode: Option[Long],
      organisationLatestDetail: GroupDetails,
      persons: Seq[IndividualSummary]
) {

  def toGroupAccount =
    GroupAccount(
      id,
      governmentGatewayGroupId,
      organisationLatestDetail.organisationName,
      organisationLatestDetail.addressUnitId,
      organisationLatestDetail.organisationEmailAddress,
      organisationLatestDetail.organisationTelephoneNumber.getOrElse("not set"),
      organisationLatestDetail.representativeFlag,
      representativeCode.filter(_ => organisationLatestDetail.representativeFlag)
    )
}

case class IndividualSummary(personLatestDetail: APIIndividualDetails)

case class GroupDetails(
      addressUnitId: Long,
      representativeFlag: Boolean,
      organisationName: String,
      organisationEmailAddress: String,
      organisationTelephoneNumber: Option[String]
)

object GroupDetails {
  implicit val format: OFormat[GroupDetails] = Json.format[GroupDetails]
}

object IndividualSummary {
  implicit val format: OFormat[IndividualSummary] = Json.format[IndividualSummary]
}

object APIDetailedGroupAccount {
  implicit val format: OFormat[APIDetailedGroupAccount] = Json.format[APIDetailedGroupAccount]
}
