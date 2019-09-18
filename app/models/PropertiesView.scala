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

package models

import java.time.{Instant, LocalDate}

import models.modernised.ValuationHistory
import models.modernised.externalpropertylink.myclients.PropertyLinkWithClient
import models.modernised.externalpropertylink.myorganisations.PropertyLinkWithAgents
import play.api.libs.json._

case class PropertiesView(authorisationOwnerOrganisationId: Long,
                          authorisationId: Long,
                          uarn: Long,
                          authorisationStatus: String,
                          startDate: LocalDate,
                          endDate: Option[LocalDate],
                          submissionId: String,
                          address: Option[String],
                          NDRListValuationHistoryItems: Seq[APIValuationHistory],
                          parties: Seq[APIParty],
                          agents: Option[Seq[LegacyParty]]) {

  def upperCase: PropertiesView = this.copy(NDRListValuationHistoryItems = NDRListValuationHistoryItems.map(_.capatalise))

  def hasValidStatus: Boolean = {
    !Seq("DECLINED", "REVOKED", "MORE_EVIDENCE_REQUIRED").contains(authorisationStatus.toUpperCase)
  }
}

object PropertiesView {
  implicit val instantReads: Reads[Instant] = Reads.instantReads("yyyy-MM-dd'T'HH:mm:ss.SSS[XXX][X]")
  implicit val format: Format[PropertiesView] = Json.format[PropertiesView]

  def apply(propertyLink: PropertyLinkWithClient, history: Seq[ValuationHistory])
  : PropertiesView =
    PropertiesView(
      authorisationOwnerOrganisationId = 1L, //TODO FIX THIS PERHAC
      authorisationId = propertyLink.authorisationId,
      uarn = propertyLink.uarn,
      address = Some(propertyLink.address),
      authorisationStatus = propertyLink.status.toString,
      startDate = propertyLink.startDate,
      endDate = propertyLink.endDate,
      submissionId = propertyLink.submissionId,
      NDRListValuationHistoryItems = history.map(history => APIValuationHistory(history)).toList,
      parties = Seq(),
      agents = Some(Seq()))


  def apply(propertyLink: PropertyLinkWithAgents, history: Seq[ValuationHistory])
  : PropertiesView =
    PropertiesView(
      authorisationOwnerOrganisationId = 1L, // TODO FIX THIS PERHAC
      authorisationId = propertyLink.authorisationId,
      uarn = propertyLink.uarn,
      address = Some(propertyLink.address),
      authorisationStatus = propertyLink.status.toString,
      startDate = propertyLink.startDate,
      endDate = propertyLink.endDate,
      submissionId = propertyLink.submissionId,
      NDRListValuationHistoryItems = history.map(history => APIValuationHistory(history)).toList,
      parties = propertyLink.agents.map(agent => APIParty(id = agent.authorisedPartyId,
        authorisedPartyStatus = agent.status,
        authorisedPartyOrganisationId = agent.organisationId,
        permissions = Seq(Permissions(agent.authorisedPartyId, agent.checkPermission, agent.challengePermission, None)))),
      agents = Some(propertyLink.agents.map(agent => LegacyParty(
        authorisedPartyId = agent.authorisedPartyId,
        agentCode = agent.representativeCode,
        organisationName = agent.organisationName,
        organisationId = agent.organisationId,
        checkPermission = agent.checkPermission,
        challengePermission = agent.challengePermission))))


}
