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

import org.joda.time.DateTime
import play.api.libs.json.Json

case class DetailedPropertyLink(authorisationId: Long,
                                uarn: Long,
                                organisationId: Int,
                                address: String,
                                capacityDeclaration: CapacityDeclaration,
                                linkedDate: DateTime,
                                pending: Boolean,
                                assessment: Seq[Assessment],
                                agents:Seq[Party]) {
}


object DetailedPropertyLink {
  implicit val formats = Json.format[DetailedPropertyLink]

  def fromAPIDashboardPropertyView(prop: APIDashboardPropertyView, parties: Seq[Party]) = {
    val capacityDeclaration = CapacityDeclaration(prop.authorisationOwnerCapacity, prop.startDate, prop.endDate)
    DetailedPropertyLink(prop.authorisationId, prop.uarn, prop.authorisationOwnerOrganisationId,
      prop.NDRListValuationHistoryItems.headOption.map(_.address).getOrElse("No address found"),
      capacityDeclaration, prop.createDatetime,
      prop.authorisationStatus != "APPROVED",
      prop.NDRListValuationHistoryItems.map(x => Assessment.fromAPIValuationHistory(x, prop.authorisationId, capacityDeclaration)),
      parties
    )
  }

}
