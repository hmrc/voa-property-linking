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

package models

import play.api.libs.json.Json

case class ClientProperty(ownerOrganisationName: String,
                          uarn: Long,
                          billingAuthorityReference: String,
                          authorisedPartyId: Long,
                          authorisationId: Long,
                          authorisationStatus: Boolean,
                          authorisedPartyStatus: String,
                          permissionId: Long,
                          checkPermission: String,
                          challengePermission: String,
                          address: String)

object ClientProperty {
  implicit val format = Json.format[ClientProperty]

  def build(prop: PropertiesView, userAccount: Option[GroupAccount]) = {
    ClientProperty(
      userAccount.map(_.companyName).getOrElse("Name not found"),
      prop.uarn,
      prop.NDRListValuationHistoryItems.headOption.map(_.billingAuthorityReference).getOrElse("BARef not found"),
      prop.parties.head.id,
      prop.authorisationId,
      prop.authorisationStatus != "APPROVED",
      prop.parties.head.authorisedPartyStatus,
      prop.parties.head.permissions.head.id,
      prop.parties.head.permissions.head.checkPermission,
      prop.parties.head.permissions.head.challengePermission,
      prop.NDRListValuationHistoryItems.headOption.map(_.address).getOrElse("Address not found")
    )

  }
}
