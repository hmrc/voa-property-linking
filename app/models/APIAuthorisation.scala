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

package uk.gov.voa.businessrates.dataplatform.stub.models

import models.{APIParty}
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json.{Format, Json, Reads, Writes}

case class APIAuthorisation(id: Option[Long],
                            uarn: Long,
                            authorisationOwnerOrganisationId: Long,
                            authorisationOwnerPersonId: Option[Long],
                            authorisationStatus: String,
                            authorisationMethod: String,
                            authorisationOwnerCapacity: String,
                            createDatetime: DateTime,
                            startDate: LocalDate,
                            endDate: Option[LocalDate],
                            submissionId: String,
                            parties: Option[Seq[APIParty]]
                           )

object APIAuthorisation {
  implicit val jodaDateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  implicit val jodaDateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd'T'HH:mm:ss.SSSZ")

  implicit val format: Format[APIAuthorisation] = Json.format[APIAuthorisation]

}
