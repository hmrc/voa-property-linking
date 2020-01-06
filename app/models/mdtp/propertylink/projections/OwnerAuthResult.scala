/*
 * Copyright 2020 HM Revenue & Customs
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

package models.mdtp.propertylink.projections

import models.searchApi.{ OwnerAuthResult => ModernisedOwnerAuthResult }
import models.modernised.externalpropertylink.myorganisations.PropertyLinksWithAgents
import play.api.libs.json.{Json, OFormat}

case class OwnerAuthResult(
                            start: Int,
                            size: Int,
                            filterTotal: Int,
                            total: Int,
                            authorisations: Seq[OwnerAuthorisation]
                          ) {

  def uppercase: OwnerAuthResult =
    this.copy(authorisations = authorisations.map(_.capatilise()))
}

object OwnerAuthResult {
  implicit val ownerAuthResult: OFormat[OwnerAuthResult] = Json.format

  def apply(authResult: ModernisedOwnerAuthResult): OwnerAuthResult = {
    OwnerAuthResult(
      start = authResult.start,
      size = authResult.size,
      filterTotal = authResult.filterTotal,
      total = authResult.total,
      authorisations = authResult.authorisations.map(OwnerAuthorisation.apply)
    )
  }
  def apply(propertLinks: PropertyLinksWithAgents)
  : OwnerAuthResult = OwnerAuthResult(start = propertLinks.start,
    size = propertLinks.size,
    filterTotal = propertLinks.filterTotal,
    total = propertLinks.total,
    authorisations = propertLinks.authorisations.map(auth => OwnerAuthorisation(auth)))

}
