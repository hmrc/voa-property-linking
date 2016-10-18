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
import connectors.IndividualAccount
import play.api.libs.json.Json
import play.api.mvc.Action

object IndividualAccountController extends PropertyLinkingBaseController {
  val individuals = Wiring().individualAccounts

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[IndividualAccount] { acc =>
      individuals.create(acc) map { _ => Created }
    }
  }

  def get() = Action.async {implicit request =>
    individuals.get().map(x => Ok(Json.toJson(x)))
  }

  def getById(id: String) = Action.async { implicit request =>
    individuals.get(id) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

}
