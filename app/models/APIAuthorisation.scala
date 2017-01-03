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
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class APIAuthorisation(
                             uarn: Long,
                             authorisationOwnerOrganisationId: Long,
                             authorisationOwnerPersonId: Long,
                             authorisationStatus: String,
                             authorisationMethod: String,
                             authorisationOwnerCapacity: String,
                             createDateTime: DateTime,
                             startDate: DateTime,
                             endDate: Option[DateTime],
                             submissionId: String
)

object APIAuthorisation {
  implicit val yourJodaDateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val reads: Reads[APIAuthorisation] =  {
    (
      (JsPath \ "uarn").read[Long] and
        (JsPath \ "authorisationOwnerOrganisationId").read[Long] and
        (JsPath \ "authorisationOwnerPersonId").read[Long] and
        (JsPath \ "authorisationStatus").read[String] and
        (JsPath \ "authorisationMethod").read[String] and
        (JsPath \ "authorisationOwnerCapacity").read[String] and
        (JsPath \ "createDatetime").read[DateTime] and
        (JsPath \ "startDate").read[DateTime] and
        (JsPath \ "endDate").readNullable[DateTime] and
        (JsPath \ "submissionId").read[String]
      )(APIAuthorisation.apply _)
  }
}
