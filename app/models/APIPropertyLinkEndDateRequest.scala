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

import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{Json, Writes}

case class APIPropertyLinkEndDateRequest(endDate: LocalDate)

object APIPropertyLinkEndDateRequest {
  implicit val yourJodaDateTimeReads: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val format = Json.format[APIPropertyLinkEndDateRequest]

  def fromPropertyLinkEndDateRequest(propertyLinkEndDateRequest: PropertyLinkEndDateRequest) = {
    APIPropertyLinkEndDateRequest(propertyLinkEndDateRequest.endDate)
  }
}