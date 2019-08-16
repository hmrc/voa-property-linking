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
import javax.inject.{Inject, Named}
import models._
import models.modernised.PropertyLinkStatus
import models.modernised.externalpropertylink.myclients.ClientPropertyLink
import models.modernised.externalpropertylink.myorganisations.OwnerPropertyLink
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{AuthorisationManagementApi, ExternalPropertyLinkApi, ExternalValuationManagementApi, MdtpDashboardManagementApi}

import scala.concurrent.{ExecutionContext, Future}

class AssessmentService @Inject()(
                                   val propertyLinksConnector: ExternalPropertyLinkApi,
                                   val externalValuationManagementApi: ExternalValuationManagementApi,
                                   val authorisationManagementApi: AuthorisationManagementApi,
                                   val mdtpDashboardManagementApi: MdtpDashboardManagementApi,
                                   @Named("authedAssessmentEndpointEnabled") val authedAssessmentEndpointEnabled: Boolean
                                 )(implicit executionContext: ExecutionContext){


  def getMyOrganisationsAssessments(submissionId: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, None)
  }

  def getClientsAssessments(submissionId: String)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      history <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, None)
  }

  def getMyOrganisationsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    authedAssessmentEndpointEnabled match {
      case false => legacyGetAssessmentsAgent(submissionId, authorisationId)
      case true => getNewMyOrganisationsAssessmentsWithCapacity(submissionId, authorisationId)
    }
  }

  def getClientsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    authedAssessmentEndpointEnabled match {
      case false => legacyGetAssessmentsClient(submissionId, authorisationId)
      case true => getNewClientsAssessmentsWithCapacity(submissionId, authorisationId)
    }
  }

   private def getNewMyOrganisationsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      capacity <- OptionT(authorisationManagementApi.getCapacity(authorisationId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(capacity.authorisationOwnerCapacity))
  }

  private def getNewClientsAssessmentsWithCapacity(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      capacity <- OptionT(authorisationManagementApi.getCapacity(authorisationId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(capacity.authorisationOwnerCapacity))
  }

  private def legacyGetAssessmentsClient(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink: ClientPropertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      capacity: Capacity <- OptionT(authorisationManagementApi.getCapacity(authorisationId))
      propertiesView <- OptionT(mdtpDashboardManagementApi.getAssessment(authorisationId))
    } yield Assessments(
      propertyLink.authorisation.authorisationId,
      propertyLink.authorisation.submissionId,
      uarn = propertyLink.authorisation.uarn,
      address = propertyLink.authorisation.address,
      pending = propertyLink.authorisation.status != PropertyLinkStatus.APPROVED,
      capacity = Some(capacity.authorisationOwnerCapacity),
      assessments = propertiesView.NDRListValuationHistoryItems.map(x => Assessment.fromAPIValuationHistory(x, propertyLink.authorisation.authorisationId)),
      agents = Seq.empty
    )
  }

  private def legacyGetAssessmentsAgent(submissionId: String, authorisationId: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_], F: cats.Functor[scala.concurrent.Future], f: cats.Monad[scala.concurrent.Future]): OptionT[Future, Assessments] = {
    for {
      propertyLink: OwnerPropertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      capacity: Capacity <- OptionT(authorisationManagementApi.getCapacity(authorisationId))
      propertiesView <- OptionT(mdtpDashboardManagementApi.getAssessment(authorisationId))
    } yield Assessments(
      propertyLink.authorisation.authorisationId,
      propertyLink.authorisation.submissionId,
      uarn = propertyLink.authorisation.uarn,
      address = propertyLink.authorisation.address,
      pending = propertyLink.authorisation.status != PropertyLinkStatus.APPROVED,
      capacity = Some(capacity.authorisationOwnerCapacity),
      assessments = propertiesView.NDRListValuationHistoryItems.map(x => Assessment.fromAPIValuationHistory(x, propertyLink.authorisation.authorisationId)),
      agents = propertyLink.authorisation.agents.map(Party(_))
    )
  }
}
