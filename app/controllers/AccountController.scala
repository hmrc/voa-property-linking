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
import connectors.ServiceContract.Account
import play.api.libs.json.Json
import play.api.mvc.Action

object AccountController extends PropertyLinkingBaseController {
  val accountConnector = Wiring().accountConnector

  def create() = Action.async{ implicit request =>
    val account: Account = request.body.asJson.get.as[Account]
    accountConnector.create(account).map(x => Created(""))
  }

  def get() = Action.async{implicit request =>
    accountConnector.get().map(x => Ok(Json.toJson(x)))
  }

}
