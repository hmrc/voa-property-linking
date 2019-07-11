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

package services

import cats.data.OptionT
import connectors.{ExternalPropertyLinkConnector, ExternalValuationManagementApi, PropertyLinkingConnector}
import models.{Assessments, ModernisedEnrichedRequest, PropertiesView}
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class AssessmentService @Inject()(
                                   val propertyLinksConnector: ExternalPropertyLinkConnector,
                                   val externalValuationManagementApi: ExternalValuationManagementApi,
                                   val legacyPropertyLinksConnector: PropertyLinkingConnector
                                 )(implicit executionContext: ExecutionContext){


  def getMyOrganisationsAssessments(submissionId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_], ec: ExecutionContext, F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, None)
  }

  def getClientsAssessments(submissionId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_], ec: ExecutionContext, F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, None)
  }

  def getMyOrganisationsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_], ec: ExecutionContext, F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      capacity <- OptionT(legacyPropertyLinksConnector.getCapacity(authorisationId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(capacity.authorisationOwnerCapacity))
  }

  def getClientsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_], ec: ExecutionContext, F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      capacity <- OptionT(legacyPropertyLinksConnector.getCapacity(authorisationId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(capacity.authorisationOwnerCapacity))
  }
}
