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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{AuthorisationManagementApi, ExternalPropertyLinkApi, ExternalValuationManagementApi, MdtpDashboardManagementApi}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class AssessmentService @Inject()(
                                   val propertyLinksConnector: ExternalPropertyLinkApi,
                                   val externalValuationManagementApi: ExternalValuationManagementApi,
                                   @Named("authedAssessmentEndpointEnabled") val authedAssessmentEndpointEnabled: Boolean
                                 )(implicit executionContext: ExecutionContext) extends Cats {


  def getMyOrganisationsAssessments(
                                     submissionId: String
                                   )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(propertyLink.authorisation.capacity))
  }

  def getClientsAssessments(
                             submissionId: String
                           )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): OptionT[Future, Assessments] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      history <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield Assessments(propertyLink.authorisation, history.NDRListValuationHistoryItems, Some(propertyLink.authorisation.capacity))
  }
}
