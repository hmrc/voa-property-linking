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

package models.modernised.addressmanagement

import play.api.libs.json.{Json, OFormat}

case class DetailedAddress(
      addressUnitId: Option[Long] = None,
      nonAbpAddressId: Option[Long] = None,
      organisationName: Option[String] = None,
      departmentName: Option[String] = None,
      subBuildingName: Option[String] = None,
      buildingName: Option[String] = None,
      buildingNumber: Option[String] = None,
      dependentThoroughfareName: Option[String] = None,
      thoroughfareName: Option[String] = None,
      doubleDependentLocality: Option[String] = None,
      dependentLocality: Option[String] = None,
      postTown: String,
      postcode: String
) {

  def simplify: SimpleAddress = {
    def concatenate(lines: Option[String]*): String = lines.toSeq.flatten.mkString(", ")

    SimpleAddress(
      addressUnitId = addressUnitId,
      line1 = concatenate(organisationName, departmentName),
      line2 = concatenate(subBuildingName, buildingName, buildingNumber),
      line3 = concatenate(dependentThoroughfareName, thoroughfareName),
      line4 = concatenate(doubleDependentLocality, dependentLocality, Some(postTown)),
      postcode = postcode
    )
  }

}

object DetailedAddress {
  implicit val formats: OFormat[DetailedAddress] = Json.format
}
