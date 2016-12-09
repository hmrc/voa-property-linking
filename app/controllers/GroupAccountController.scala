/*
 * Copyright 2016 HM Revenue & Customs
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

import config.Wiring
import models.GroupAccountSubmission
import play.api.libs.json.Json
import play.api.mvc.Action

object GroupAccountController extends PropertyLinkingBaseController {
  val groups = Wiring().groupAccounts

  def getById(id: String) = Action.async { implicit request =>
    groups.findByGGID(id) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def withAgentCode(agentCode: String) = Action.async { implicit request =>
    groups.withAgentCode(agentCode) map {
      case Some(a) => Ok(Json.toJson(a))
      case None => NotFound
    }
  }

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[GroupAccountSubmission] { acc =>
      groups.create(acc) map { Created(_) }
    }
  }
}
