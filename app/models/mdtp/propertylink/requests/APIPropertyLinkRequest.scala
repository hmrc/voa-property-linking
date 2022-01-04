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

package models.mdtp.propertylink.requests

import java.time.{Instant, LocalDate}

import models.FileInfo
import play.api.libs.json.{Json, OFormat, Reads}

case class APIPropertyLinkRequest(
      uarn: Long,
      authorisationOwnerOrganisationId: Long,
      authorisationOwnerPersonId: Long,
      createDatetime: Instant,
      authorisationMethod: String,
      uploadedFiles: Seq[FileInfo],
      submissionId: String,
      authorisationOwnerCapacity: String,
      startDate: LocalDate,
      endDate: Option[LocalDate] = None)

object APIPropertyLinkRequest {
  implicit val instantReads: Reads[Instant] = Reads.instantReads("yyyy-MM-dd'T'HH:mm:ss.SSS[XXX][X]")
  implicit val format: OFormat[APIPropertyLinkRequest] = Json.format[APIPropertyLinkRequest]

  def fromPropertyLinkRequest(propertyLinkRequest: PropertyLinkRequest) =
    APIPropertyLinkRequest(
      uarn = propertyLinkRequest.uarn,
      authorisationOwnerOrganisationId = propertyLinkRequest.organisationId,
      authorisationOwnerPersonId = propertyLinkRequest.individualId,
      createDatetime = propertyLinkRequest.linkedDate,
      authorisationMethod = propertyLinkRequest.linkBasis,
      uploadedFiles = propertyLinkRequest.fileInfo,
      submissionId = propertyLinkRequest.submissionId,
      authorisationOwnerCapacity = propertyLinkRequest.capacityDeclaration.capacity,
      startDate = propertyLinkRequest.capacityDeclaration.fromDate,
      endDate = propertyLinkRequest.capacityDeclaration.toDate
    )
}
