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

import java.time.LocalDate

import models.modernised.externalpropertylink.myclients.PropertyLinkWithClient
import models.modernised.externalpropertylink.myorganisations.PropertyLinkWithAgents
import models.modernised.{PropertyLinkStatus, ValuationHistory}
import play.api.libs.json.Json

case class Assessment(
                       authorisationId: Long,
                       assessmentRef: Long,
                       listYear: String,
                       uarn: Long,
                       effectiveDate: Option[LocalDate],
                       rateableValue: Option[Long],
                       address: PropertyAddress,
                       billingAuthorityReference: String,
                       currentFromDate: Option[LocalDate],
                       currentToDate: Option[LocalDate]

                     )


case class Assessments(
                        authorisationId: Long,
                        submissionId: String,
                        uarn: Long,
                        address: String,
                        pending: Boolean,
                        capacity: Option[String],
                        assessments: Seq[Assessment],
                        agents: Seq[Party])



object Assessment {
  implicit val formats = Json.format[Assessment]

  def fromAPIValuationHistory(
                               valuationHistory: APIValuationHistory,
                               authorisationId: Long) = {
    Assessment(
      authorisationId,
      valuationHistory.asstRef,
      valuationHistory.listYear,
      valuationHistory.uarn,
      valuationHistory.effectiveDate,
      valuationHistory.rateableValue,
      PropertyAddress.fromString(valuationHistory.address),
      valuationHistory.billingAuthorityReference,
      valuationHistory.currentFromDate,
      valuationHistory.currentToDate
    )
  }

  def fromValuationHistory(valuationHistory: ValuationHistory, authorisationId: Long) = {
    Assessment(
      authorisationId,
      valuationHistory.asstRef,
      valuationHistory.listYear,
      valuationHistory.uarn,
      valuationHistory.effectiveDate,
      valuationHistory.rateableValue.map {d => d.longValue()},
      PropertyAddress.fromString(valuationHistory.address),
      valuationHistory.billingAuthorityReference,
      valuationHistory.currentFromDate,
      valuationHistory.currentToDate
    )
  }

}

object Assessments {
  implicit val formats = Json.format[Assessments]

  def apply(propertyLink: PropertyLinkWithAgents, history: Seq[ValuationHistory], capacity: Option[String])
    :Assessments =
    Assessments(
      propertyLink.authorisationId,
      propertyLink.submissionId,
      propertyLink.uarn,
      history.headOption.map(_.address).getOrElse("No address found"),
      propertyLink.status != PropertyLinkStatus.APPROVED,
      capacity = capacity,
      assessments = history.map(x => Assessment.fromValuationHistory(x, propertyLink.authorisationId)),
      agents = propertyLink.agents.map(agent => Party(agent.authorisedPartyId,
        agent.representativeCode,
        agent.organisationName,
        agent.organisationId,
        agent.checkPermission,
        agent.challengePermission))
    )

  def apply(propertyLink: PropertyLinkWithClient, history: Seq[ValuationHistory], capacity: Option[String])
  :Assessments =
    Assessments(
      propertyLink.authorisationId,
      propertyLink.submissionId,
      propertyLink.uarn,
      history.headOption.map(_.address).getOrElse("No address found"),
      propertyLink.status != PropertyLinkStatus.APPROVED,
      capacity = capacity,
      history.map(x => Assessment.fromValuationHistory(x, propertyLink.authorisationId)),
      Seq()
    )
}
