/*
 * Copyright 2023 HM Revenue & Customs
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

import models.modernised.AllowedAction.AllowedAction

import java.time.LocalDate
import models.modernised.ListType.ListType
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
      billingAuthorityCode: Option[String],
      listType: ListType,
      allowedActions: List[AllowedAction],
      currentFromDate: Option[LocalDate],
      currentToDate: Option[LocalDate]
)

case class Assessments(
      authorisationId: Long,
      submissionId: String,
      uarn: Long,
      address: String,
      pending: Boolean,
      clientOrgName: Option[String],
      capacity: Option[String],
      assessments: Seq[Assessment],
      agents: Seq[Party])

object Assessment {
  implicit val formats = Json.format[Assessment]

  def fromValuationHistory(valuationHistory: ValuationHistory, authorisationId: Long): Assessment =
    Assessment(
      authorisationId = authorisationId,
      assessmentRef = valuationHistory.asstRef,
      listYear = valuationHistory.listYear,
      uarn = valuationHistory.uarn,
      effectiveDate = valuationHistory.effectiveDate,
      rateableValue = valuationHistory.rateableValue.map(_.longValue),
      address = PropertyAddress.fromString(valuationHistory.address),
      billingAuthorityReference = valuationHistory.billingAuthorityReference,
      billingAuthorityCode = valuationHistory.billingAuthCode,
      listType = valuationHistory.listType,
      allowedActions = valuationHistory.allowedActions,
      currentFromDate = valuationHistory.currentFromDate,
      currentToDate = valuationHistory.currentToDate
    )

}

object Assessments {
  implicit val formats = Json.format[Assessments]

  def apply(
        propertyLink: PropertyLinkWithAgents,
        history: Seq[ValuationHistory],
        capacity: Option[String]): Assessments =
    Assessments(
      propertyLink.authorisationId,
      propertyLink.submissionId,
      propertyLink.uarn,
      history.headOption.map(_.address).getOrElse("No address found"),
      propertyLink.status != PropertyLinkStatus.APPROVED,
      clientOrgName = None,
      capacity = capacity,
      assessments = history.map(x => Assessment.fromValuationHistory(x, propertyLink.authorisationId)),
      agents = propertyLink.agents.map(
        agent =>
          Party(
            agent.authorisedPartyId,
            agent.representativeCode,
            agent.organisationName,
            agent.organisationId
        ))
    )

  def apply(
        propertyLink: PropertyLinkWithClient,
        history: Seq[ValuationHistory],
        capacity: Option[String]): Assessments =
    Assessments(
      authorisationId = propertyLink.authorisationId,
      submissionId = propertyLink.submissionId,
      uarn = propertyLink.uarn,
      address = history.headOption.map(_.address).getOrElse("No address found"),
      pending = propertyLink.status != PropertyLinkStatus.APPROVED,
      clientOrgName = Some(propertyLink.client.organisationName),
      capacity = capacity,
      assessments = history.map(x => Assessment.fromValuationHistory(x, propertyLink.authorisationId)),
      agents = Seq()
    )
}
