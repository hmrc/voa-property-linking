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

package uk.gov.hmrc.voapropertylinking.services

import cats.data.OptionT

import javax.inject.Inject
import models._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.{ExternalPropertyLinkApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{ModernisedExternalPropertyLinkApi, ModernisedExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class AssessmentService @Inject() (
      val modernisedPropertyLinksConnector: ModernisedExternalPropertyLinkApi,
      val modernisedValuationManagementApi: ModernisedExternalValuationManagementApi,
      propertyLinksConnector: ExternalPropertyLinkApi,
      valuationManagementApi: ExternalValuationManagementApi,
      featureSwitch: FeatureSwitch
)(implicit executionContext: ExecutionContext)
    extends Cats {

  def getMyOrganisationsAssessments(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, Assessments] =
    for {
      propertyLink <- if (featureSwitch.isBstDownstreamEnabled)
                        OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
                      else
                        OptionT(modernisedPropertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history <- if (featureSwitch.isBstDownstreamEnabled)
                   OptionT(valuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
                 else
                   OptionT(
                     modernisedValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId)
                   )
    } yield Assessments(
      propertyLink.authorisation,
      history.NDRListValuationHistoryItems,
      Some(propertyLink.authorisation.capacity)
    )

  def getClientsAssessments(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, Assessments] =
    for {
      propertyLink <- if (featureSwitch.isBstDownstreamEnabled)
                        OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
                      else
                        OptionT(modernisedPropertyLinksConnector.getClientsPropertyLink(submissionId))
      history <- if (featureSwitch.isBstDownstreamEnabled)
                   OptionT(valuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
                 else
                   OptionT(
                     modernisedValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId)
                   )
    } yield Assessments(
      propertyLink = propertyLink.authorisation,
      history = history.NDRListValuationHistoryItems,
      capacity = Some(propertyLink.authorisation.capacity)
    )
}
