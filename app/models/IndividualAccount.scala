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

package models

import org.joda.time.LocalDate
import play.api.libs.json.Json

case class IndividualDetails(firstName: String, lastName: String, email: String, phone1: String, phone2: Option[String])

object IndividualDetails {
  implicit val formats = Json.format[IndividualDetails]
}

case class IndividualAccount(externalId: String, trustId: String, organisationId: Int, details: IndividualDetails) {
  def toAPIIndividualAccount = {
    APIIndividualAccount(trustId, details.firstName, details.lastName, organisationId, 1, details.phone1, details.phone2, details.email, externalId, LocalDate.now)
  }
}

object IndividualAccount {
  implicit val formats = Json.format[IndividualAccount]
}
