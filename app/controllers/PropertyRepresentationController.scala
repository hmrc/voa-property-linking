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

import config.Wiring
import models._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Action

import scala.concurrent.Future

object PropertyRepresentationController extends PropertyLinkingBaseController {
  val representations = Wiring().propertyRepresentationConnector

  def validateAgentCode(agentCode:Long, authorisationId: Long) = Action.async { implicit request =>
    representations.validateAgentCode(agentCode, authorisationId).map(
      _.fold(
        orgId => Ok(s"""{"organisationId": $orgId}"""),
        errorString => Ok(s"""{"failureCode": "$errorString"}""")
      )
    )
  }

  def forAgent(status: String, organisationId: Long) = Action.async { implicit request =>
    representations.forAgent(status, organisationId).map( x=> Ok(Json.toJson(x)))
  }

  def get(representationId: String) = Action.async { implicit request =>
    representations.get(representationId) map {
      case Some(r) => Ok(Json.toJson(r))
      case None => BadRequest
    }
  }

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[RepresentationRequest] { reprRequest =>
      representations.create(APIRepresentationRequest.fromRepresentationRequest(reprRequest)).map(x => Ok(""))
    }
  }

  def response() = Action.async(parse.json) { implicit request =>
    withJsonBody[APIRepresentationResponse] { r =>
      representations.response(r) map { _ => Ok("") }
    }
  }

  def update() = Action.async(parse.json) { implicit request =>
    withJsonBody[UpdatedRepresentation] { r =>
      representations.update(r) map { _ => Ok("") }
    }
  }

}
