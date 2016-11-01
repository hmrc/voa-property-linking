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
import play.api.libs.json.Json

object ServiceContract {

  case class PropertyRepresentation(representationId: String, agentId: String, userId: String, uarn: Long,
                                    canCheck: Boolean, canChallenge: Boolean, pending: Boolean)

  case class CapacityDeclaration(capacity: String, fromDate: DateTime, toDate: Option[DateTime] = None)

  case class PropertyLink(uarn: Long, userId: String, capacityDeclaration: CapacityDeclaration,
                          linkedDate: DateTime, linkBasis: String,
                          specialCategoryCode: String, description: String, bulkClassIndicator: String,
                          pending: Boolean)

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

case class IndividualAccount(id: String, groupId: String)

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

