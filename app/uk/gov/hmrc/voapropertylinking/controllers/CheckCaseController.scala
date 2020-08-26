/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ExternalCaseManagementApi
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myclients.CheckCasesWithClient
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myorganisation.CheckCasesWithAgent

import scala.concurrent.{ExecutionContext, Future}

/*
  TODO this controller will move to check backend after planned migration to external case management api
 */
class CheckCaseController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      externalCaseManagementApi: ExternalCaseManagementApi
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def getCheckCases(submissionId: String, party: String): Action[AnyContent] = authenticated.async { implicit request =>
    party match {
      case "agent"  => getMyClientsCheckCases(propertyLinkSubmissionId = submissionId)
      case "client" => getMyOrganisationCheckCases(propertyLinkSubmissionId = submissionId)
      case p        => Future.successful(NotImplemented(s"invalid party (projection) supplied: $p"))
    }
  }

  private def getMyOrganisationCheckCases(propertyLinkSubmissionId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Result] =
    externalCaseManagementApi
      .getMyOrganisationCheckCases(propertyLinkSubmissionId)
      .recover {
        case e: Throwable =>
          logger.warn("get my organisation check cases returned unexpected exception", e)
          CheckCasesWithAgent(1, 100, 0, 0, Nil) // I believe this shouldnt be handled here. I think this should return the error it got.
      }
      .map(response => Ok(Json.toJson(response)))

  private def getMyClientsCheckCases(propertyLinkSubmissionId: String)(
        implicit request: RequestWithPrincipal[_]): Future[Result] =
    externalCaseManagementApi
      .getMyClientsCheckCases(propertyLinkSubmissionId)
      .recover {
        case e: Throwable =>
          logger.warn("get my clients check cases returned unexpected exception", e)
          CheckCasesWithClient(1, 100, 0, 0, Nil) // I believe this shouldnt be handled here. I think this should return the error it got.
      }
      .map(response => Ok(Json.toJson(response)))

}
