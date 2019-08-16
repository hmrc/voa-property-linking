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

package models.voa.propertylinking.requests

import java.time.{LocalDate, LocalDateTime}

import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.Capacity.Capacity
import models.modernised.ProvidedEvidence.{ProvidedEvidence => Method}
import models.modernised._
import play.api.libs.json.{Json, OFormat, Writes}
import uk.gov.hmrc.voapropertylinking.utils.Formatters
import uk.gov.hmrc.voapropertylinking.utils.FileNameSanitisationUtils.formatFileName

case class CreatePropertyLink(
                               uarn: Long,
                               capacity: Capacity,
                               startDate: LocalDate,
                               endDate: Option[LocalDate],
                               method: Method,
                               PLsubmissionId: String,
                               createDatetime: LocalDateTime,
                               uploadedFiles: Seq[Evidence],
                               submissionSource: String)

object CreatePropertyLink {

  implicit val voaDateTimeWrites: Writes[LocalDateTime] = Formatters.writes
  implicit val format: OFormat[CreatePropertyLink] = Json.format

  def apply(request: APIPropertyLinkRequest):CreatePropertyLink =
    CreatePropertyLink(
      uarn = request.uarn,
      capacity = Capacity.withName(request.authorisationOwnerCapacity),
      startDate = request.startDate,
      endDate = request.endDate,
      method = ProvidedEvidence.withName(request.authorisationMethod),
      PLsubmissionId = request.submissionId,
      createDatetime = LocalDateTime.now(),
      uploadedFiles = request.uploadedFiles.map(e => Evidence(formatFileName(request.submissionId, e.name), EvidenceType.withName(e.evidenceType))),
      submissionSource = "DFE_UI")
}
