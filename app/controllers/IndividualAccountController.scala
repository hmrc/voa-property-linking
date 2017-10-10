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

import auth.Authenticated
import connectors.auth.AuthConnector
import connectors.{BusinessRatesAuthConnector, IndividualAccountConnector}
import models.IndividualAccountSubmission
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Results.EmptyContent

class IndividualAccountController @Inject() ( val auth: AuthConnector,
                                              individuals: IndividualAccountConnector,
                                             brAuth: BusinessRatesAuthConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[IndividualAccountSubmission] { acc =>
      individuals.create(acc) map { x => Created(x) }
    }
  }

  def update(personId: Long) = authenticated(parse.json) { implicit request =>
    withJsonBody[IndividualAccountSubmission] { account =>
      for {
        _ <- individuals.update(personId, account)
        _ <- brAuth.clearCache()
      } yield {
        Ok(EmptyContent())
      }
    }
  }

  def get(personId: Long) = authenticated { implicit request =>
    individuals.get(personId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def withExternalId(externalId: String) = authenticated { implicit request =>
    individuals.findByGGID(externalId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

}
