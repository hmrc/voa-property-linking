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

package models

import java.time.Instant

import play.api.libs.json.{Json, OFormat, Reads}

case class APIRepresentationRequest(
      authorisationId: Long,
      submissionId: String,
      authorisedPartyOrganisationId: Long,
      authorisationOwnerPersonId: Long,
      createDatetime: Instant
)

object APIRepresentationRequest {
  implicit val instantReads: Reads[Instant] = Reads.instantReads("yyyy-MM-dd'T'HH:mm:ss.SSS[XXX][X]")
  implicit val format: OFormat[APIRepresentationRequest] = Json.format[APIRepresentationRequest]

  def fromRepresentationRequest(reprRequest: RepresentationRequest) = APIRepresentationRequest(
    reprRequest.authorisationId,
    reprRequest.submissionId,
    reprRequest.agentOrganisationId,
    reprRequest.individualId,
    reprRequest.createDatetime
  )
}
