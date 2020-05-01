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
import models.searchApi.{Agent, Agents}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{AuthorisationSearchApi, ExternalPropertyLinkApi}

import scala.concurrent.ExecutionContext

class AgentController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      externalPropertyLinkApi: ExternalPropertyLinkApi
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  // organisationId parameter is not needed as we're calling
  // a new authed endpoint which will pick up current user from HeaderCarrier gg headers
  def manageAgents(organisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    externalPropertyLinkApi.getMyOrganisationsAgents().map { agentList =>
      Ok(Json.toJson(Agents(agentList.agents.map(a => Agent(a.name, a.representativeCode)))))
    }
  }
}
