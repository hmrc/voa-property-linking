/*
 * Copyright 2018 HM Revenue & Customs
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

package controllers

import javax.inject.Inject

import auth.Authenticated
import connectors.CheckCaseConnector
import connectors.auth.DefaultAuthConnector
import play.api.libs.json.Json

class CheckCaseController @Inject() (val authConnector: DefaultAuthConnector,
                                     checkCaseConnector: CheckCaseConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def getCheckCases(submissionId: String, party: String) = authenticated { implicit request =>

    checkCaseConnector.getCheckCases(submissionId, party) map {
      case Some(checkCasesResponse) => Ok(Json.toJson(checkCasesResponse))
      case _ => NotFound
    }
  }
}

