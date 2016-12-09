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
import connectors.VmvConnector
import models.{PropertyRepresentation, UpdatedRepresentation}
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Action

import scala.concurrent.Future

object PropertyRepresentationController extends PropertyLinkingBaseController {
  val representations = Wiring().propertyRepresentationConnector

  def find(linkId: String) = Action.async { implicit request =>
    representations.find(linkId) flatMap {
      case Nil => Future.successful(Ok(JsArray()))
      case r :: rs => VmvConnector.getPropertyInfo(r.uarn) map {
        case None => BadRequest
        case Some(p) => Ok(Json.toJson(r.withAddress(p.address) :: rs.map(_.withAddress(p.address))))
      }
    }
  }

  def forAgent(agentId: String) = Action.async { implicit request =>
    representations.forAgent(agentId) flatMap { reps =>
      Future.sequence {
        reps.map { r =>
          VmvConnector.getPropertyInfo(r.uarn) map {
            case Some(p) => r.withAddress(p.address)
            case None => throw new Exception(s"Invalid uarn ${r.uarn}")
          }
        }
      }
    } map { res => Ok(Json.toJson(res)) }
  }

  def get(representationId: String) = Action.async { implicit request =>
    representations.get(representationId) map {
      case Some(r) => Ok(Json.toJson(r))
      case None => BadRequest
    }
  }

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyRepresentation] { reprRequest =>
      representations.create(reprRequest).map(x => Ok(""))
    }
  }

  def update() = Action.async(parse.json) { implicit request =>
    withJsonBody[UpdatedRepresentation] { r =>
      representations.update(r) map { _ => Ok("") }
    }
  }

  def accept(reprId: String) = Action.async { implicit request =>
    representations.accept(reprId).map(x => Ok(""))
  }

  def reject(reprId: String) = Action.async { implicit request =>
    representations.reject(reprId).map(x => Ok(""))
  }

}
