/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.IndividualAccountConnector
import models.IndividualAccountWrite
import play.api.libs.json.Json
import play.api.mvc.Action

class IndividualAccountController @Inject() (individuals: IndividualAccountConnector) extends PropertyLinkingBaseController {

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[IndividualAccountWrite] { acc =>
      individuals.create(acc) map { Created(_) }
    }
  }

  def get(personId: Int) = Action.async { implicit request =>
    individuals.get(personId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def withExternalId(externalId: String) = Action.async { implicit request =>
    individuals.findByGGID(externalId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

}
