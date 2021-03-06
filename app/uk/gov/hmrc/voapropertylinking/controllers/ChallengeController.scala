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

package uk.gov.hmrc.voapropertylinking.controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ExternalCaseManagementApi

import scala.concurrent.ExecutionContext

/*
  TODO move this to challenge backend.
 */
class ChallengeController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      externalCaseManagementApi: ExternalCaseManagementApi
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def canChallenge(
        propertyLinkSubmissionId: String,
        checkCaseRef: String,
        valuationId: Long,
        party: String): Action[AnyContent] = authenticated.async { implicit request =>
    externalCaseManagementApi
      .canChallenge(propertyLinkSubmissionId, checkCaseRef, valuationId, party)
      .map {
        case Some(resp) => Ok(Json.toJson(resp))
        case _          => Forbidden
      }
  }
}
