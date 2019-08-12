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

package controllers

import javax.inject.Inject
import auth.Authenticated
import connectors.AgentConnector
import connectors.auth.DefaultAuthConnector
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext

class AgentController @Inject()(
                                 val authConnector: DefaultAuthConnector,
                                 agentConnector: AgentConnector
                               )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController with Authenticated  {

  def manageAgents(organisationId: Long) = authenticated { implicit request =>
    agentConnector.manageAgents(organisationId) map { agents => Ok(Json.toJson(agents)) }
  }

}
