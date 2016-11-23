/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

object ServiceContract {

  case class PropertyRepresentation(representationId: String, agentId: String, agentName: String, groupId: String,
                                    groupName: String, uarn: Long, canCheck: String, canChallenge: String, pending: Boolean) {

    def withAddress(address: Address) = FrontendPropertyRepresentation(
      representationId, agentId, agentName, groupId, groupName, uarn, address, canCheck, canChallenge, pending
    )
  }

  case class CapacityDeclaration(capacity: String, fromDate: DateTime, toDate: Option[DateTime] = None)

  case class FileInfo(fileName: String, fileType: String)

  case class PropertyLinkRequest(uarn: Long, userId: String, capacityDeclaration: CapacityDeclaration,
                                 linkedDate: DateTime, linkBasis: String,
                                 specialCategoryCode: String, description: String, bulkClassIndicator: String,
                                 fileInfo: Option[FileInfo])

  case class PropertyLink(uarn: Long, userId: String, description: String, capacityDeclaration: CapacityDeclaration,
                          linkedDate: DateTime, pending: Boolean)

}

case class Address(line1: String, line2: String, line3: String, postcode: String)

object Address {
  implicit val formats = Json.format[Address]

  def fromLines(lines: Seq[String], postcode: String) = {
    def optionalLine(n: Int) = lines.lift(n).getOrElse("")

    require(lines.nonEmpty)
    Address(lines.head, optionalLine(1), optionalLine(2), postcode)
  }
}

case class IndividualDetails(firstName: String, lastName: String, email: String, phone1: String, phone2: Option[String])

object IndividualDetails {
  implicit val formats = Json.format[IndividualDetails]
}

case class IndividualAccount(id: String, groupId: String, details: IndividualDetails)

object IndividualAccount {
  implicit val formats = Json.format[IndividualAccount]
}

case class GroupAccount(id: String, companyName: String, address: Address, email: String, phone: String,
                        isSmallBusiness: Boolean, isAgent: Boolean)

object GroupAccount {
  implicit val formats = Json.format[GroupAccount]
}


case class Property(uarn: Long, billingAuthorityReference: String, address: Address, isSelfCertifiable: Boolean,
                    specialCategoryCode: String, description: String, bulkClassIndicator: String)

object Property {
  implicit val formats = Json.format[Property]
}

case class FrontendPropertyRepresentation(representationId: String, agentId: String, agentName: String, groupId: String,
                                          groupName: String, uarn: Long, address: Address, canCheck: String, canChallenge: String,
                                          pending: Boolean)

object FrontendPropertyRepresentation {
  implicit val formats = Json.format[FrontendPropertyRepresentation]
}

