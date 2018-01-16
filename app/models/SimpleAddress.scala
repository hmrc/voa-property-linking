/*
 * Copyright 2018 HM Revenue & Customs
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

case class SimpleAddress(addressUnitId: Option[Int], line1: String, line2: String, line3: String, line4: String, postcode: String) {

  def toDetailedAddress = (line1, line2, line3, line4) match {
    case (l1, l2, "", "") => DetailedAddress(buildingNumber = Some(l1), postTown = l2, postcode = postcode)
    case (l1, l2, l3, "") => DetailedAddress(buildingNumber = Some(l1), thoroughfareName = Some(l2), postTown = l3, postcode = postcode)
    case (l1, l2, l3, l4) => DetailedAddress(buildingNumber = Some(l1), thoroughfareName = Some(l2), dependentLocality = Some(l3), postTown = l4, postcode = postcode)
  }
}

object SimpleAddress {
  implicit val format = Json.format[SimpleAddress]
}
