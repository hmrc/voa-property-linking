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

import java.time.Instant

import play.api.libs.json.{Json, OWrites}

case class RepresentationRequest(
                                  authorisationId: Long,
                                  agentOrganisationId: Long,
                                  organisationId: Long,
                                  individualId: Long,
                                  submissionId: String,
                                  checkPermission: String,
                                  challengePermission: String,
                                  createDatetime: Instant
                                )

object RepresentationRequest {
  implicit val format = Json.format[RepresentationRequest]

}
object RepresentationRequestAuditWriteFormat {

  val writes: OWrites[RepresentationRequest] = OWrites[RepresentationRequest] { request =>
    Json.obj(
      "propertyLinkId" -> Json.toJson(request.authorisationId),
      "agentOrganisationId" -> Json.toJson(request.agentOrganisationId),
      "organisationId" -> Json.toJson(request.organisationId),
      "individualId" -> Json.toJson(request.individualId),
      "submissionId" -> Json.toJson(request.submissionId),
      "checkPermission" -> Json.toJson(request.checkPermission),
      "challengePermission" -> Json.toJson(request.challengePermission),
      "createDatetime" -> Json.toJson(request.createDatetime)

    )
  }
}
