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

package models.searchApi

import models.modernised.{PropertyLinksWithAgents, PropertyLinksWithClient}
import play.api.libs.json.Json

case class OwnerAuthResult(
                      start: Int,
                      size: Int,
                      filterTotal: Int,
                      total: Int,
                      authorisations: Seq[OwnerAuthorisation]
                    ){

  def uppercase = this.copy(authorisations = authorisations.map(_.capatilise()))
}

object OwnerAuthResult {
  implicit val ownerAuthResult = Json.format[OwnerAuthResult]

  def apply(propertLinks: PropertyLinksWithClient)
  : OwnerAuthResult = OwnerAuthResult(start = propertLinks.start,
    size = propertLinks.size,
    filterTotal = propertLinks.filterTotal,
    total = propertLinks.total,
    authorisations = propertLinks.authorisations.map(auth => OwnerAuthorisation(auth)))


  def apply(propertLinks: PropertyLinksWithAgents)
  : OwnerAuthResult = OwnerAuthResult(start = propertLinks.start,
    size = propertLinks.size,
    filterTotal = propertLinks.filterTotal,
    total = propertLinks.total,
    authorisations = propertLinks.authorisations.map(auth => OwnerAuthorisation(auth)))

}
