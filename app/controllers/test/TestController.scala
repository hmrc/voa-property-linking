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

package controllers.test

import controllers.PropertyLinkingBaseController
import javax.inject.Inject
import play.api.mvc.{Action, AnyContent}
import repositories.DVRRecordRepository
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.test.TestConnector

import scala.concurrent.ExecutionContext

class TestController @Inject()(
                                authenticated: AuthenticatedActionBuilder,
                                testConnector: TestConnector,
                                dvrRecordRepository: DVRRecordRepository
                              )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  /*
  Move tests away from this method.
   */
  def deleteOrganisation(organisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    testConnector
      .deleteOrganisation(organisationId)
      .map(_ => Ok)
  }

  def clearDvrRecords(organisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    dvrRecordRepository
      .clear(organisationId)
      .map(_ => Ok)
  }

  def deleteCheckCases(propertyLinkingSubmissionId: String): Action[AnyContent] = authenticated.async { implicit request =>
    testConnector
      .deleteCheckCases(propertyLinkingSubmissionId)
      .map(_ => Ok)
  }

}
