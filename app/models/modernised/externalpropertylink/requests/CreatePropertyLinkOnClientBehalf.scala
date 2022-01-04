/*
 * Copyright 2022 HM Revenue & Customs
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

package models.modernised.externalpropertylink.requests

import java.time.{LocalDate, LocalDateTime}

import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.Capacity.Capacity
import models.modernised.ProvidedEvidence.{ProvidedEvidence => Method}
import models.modernised._
import play.api.libs.json.{Json, OFormat, Writes}
import uk.gov.hmrc.voapropertylinking.utils.Formatters

case class CreatePropertyLinkOnClientBehalf(
      uarn: Long,
      capacity: Capacity,
      startDate: LocalDate,
      endDate: Option[LocalDate],
      method: Method,
      propertyLinkSubmissionId: String,
      createDatetime: LocalDateTime,
      evidence: Seq[Evidence],
      submissionSource: String)

object CreatePropertyLinkOnClientBehalf {

  implicit val voaDateTimeWrites: Writes[LocalDateTime] = Formatters.writes
  implicit val format: OFormat[CreatePropertyLinkOnClientBehalf] = Json.format

  def apply(request: APIPropertyLinkRequest): CreatePropertyLinkOnClientBehalf =
    CreatePropertyLinkOnClientBehalf(
      uarn = request.uarn,
      capacity = Capacity.withName(request.authorisationOwnerCapacity),
      startDate = request.startDate,
      endDate = request.endDate,
      method = ProvidedEvidence.withName(request.authorisationMethod),
      propertyLinkSubmissionId = request.submissionId,
      createDatetime = LocalDateTime.now(),
      evidence = request.uploadedFiles.map(e => Evidence(e.name, EvidenceType.withName(e.evidenceType))),
      submissionSource = "DFE_UI"
    )
}
