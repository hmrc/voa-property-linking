/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{ExternalPropertyLinkApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class AssessmentService @Inject()(
      val propertyLinksConnector: ExternalPropertyLinkApi,
      val externalValuationManagementApi: ExternalValuationManagementApi
)(implicit executionContext: ExecutionContext)
    extends Cats {

  def getMyOrganisationsAssessments(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, Assessments] =
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history <- OptionT(
                  externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield
      Assessments(
        propertyLink.authorisation,
        history.NDRListValuationHistoryItems,
        Some(propertyLink.authorisation.capacity))

  def getClientsAssessments(
        submissionId: String
  )(implicit request: RequestWithPrincipal[_]): OptionT[Future, Assessments] =
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
      history <- OptionT(
                  externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield
      Assessments(
        propertyLink.authorisation,
        history.NDRListValuationHistoryItems,
        Some(propertyLink.authorisation.capacity))
}
