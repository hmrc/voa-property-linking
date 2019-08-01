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

package models.searchApi

import java.time.LocalDate

import models.PropertyRepresentation
import models.modernised.externalpropertylink.myclients.PropertyLinkWithClient
import play.api.libs.json.Json

case class AgentAuthorisation(
                               authorisationId: Long,
                               authorisedPartyId: Long,
                               status: String,
                               representationSubmissionId: String,
                               submissionId: String,
                               uarn: Long,
                               address: String,
                               localAuthorityRef: String,
                               client: Client,
                               representationStatus: String,
                               checkPermission: String,
                               challengePermission: String
                             ) {

  def capitalise() = this.copy(address = address.toUpperCase)

  def toPropertyRepresentation = PropertyRepresentation(
    this.authorisationId,
    this.localAuthorityRef,
    this.representationSubmissionId,
    this.client.organisationId,
    this.client.organisationName,
    this.address,
    this.checkPermission,
    this.challengePermission,
    LocalDate.now(), //TODO This is not being shown on the frontend for now, once the modernised API changes to return this correctly, we will display it again
    this.status
  )
}

object AgentAuthorisation {
  implicit val agentAuthorisation = Json.format[AgentAuthorisation]
}
