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

package uk.gov.hmrc.voapropertylinking.controllers

import models.CanChallengeResponse

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.ExternalCaseManagementApi
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ModernisedExternalCaseManagementApi

import scala.concurrent.{ExecutionContext, Future}

/*
  TODO move this to challenge backend.
 */
class ChallengeController @Inject() (
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      modernisedExternalCaseManagementApi: ModernisedExternalCaseManagementApi,
      externalCaseManagementApi: ExternalCaseManagementApi,
      featureSwitch: FeatureSwitch
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def canChallenge(
        propertyLinkSubmissionId: String,
        checkCaseRef: String,
        valuationId: Long,
        party: String
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      val canChallengeResponse: Future[Option[CanChallengeResponse]] =
        if (featureSwitch.isBstDownstreamEnabled)
          externalCaseManagementApi
            .canChallenge(propertyLinkSubmissionId, checkCaseRef, valuationId, party)
        else
          modernisedExternalCaseManagementApi
            .canChallenge(propertyLinkSubmissionId, checkCaseRef, valuationId, party)
      canChallengeResponse.map {
        case Some(resp) => Ok(Json.toJson(resp))
        case _          => Forbidden
      }
    }
}
