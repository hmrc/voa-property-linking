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
import connectors.ServiceContract._
import play.api.libs.json.Json
import play.api.mvc.Action
import serialization.JsonFormats._
import uk.gov.hmrc.play.http.Upstream5xxResponse

object PropertyLinkingController extends PropertyLinkingBaseController {
  val propertyLinks = Wiring().propertyLinkingConnector

  def create(submissionId: String) = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { link =>
      propertyLinks.create(submissionId, link) map { _ => Created } recover { case _: Upstream5xxResponse => InternalServerError }
    }
  }

  def find(userId: String) = Action.async { implicit request =>
    propertyLinks.find(userId) map (x => Ok(Json.toJson(x)))
  }

  def get(linkId: String) = Action.async { implicit request =>
    propertyLinks.get(linkId) map { x => Ok(Json.toJson(x)) }
  }

}
