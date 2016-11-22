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
import play.api.libs.json.Json
import play.api.mvc.Action
import serialization.JsonFormats._
import connectors.ServiceContract._
import connectors.VmvConnector

import scala.concurrent.Future

object PropertyRepresentationController extends PropertyLinkingBaseController {
  val reprConnector = Wiring().propertyRepresentationConnector

  def getPropertyRepresentations(userId: String, uarn: Long) = Action.async { implicit request =>
    for {
      property <- VmvConnector.getPropertyInfo(uarn)
      reps <- reprConnector.get(userId, uarn)
    } yield {
      property match {
        case Some(p) => Ok(Json.toJson(reps.map(_.withAddress(p.address))))
        case None => BadRequest
      }
    }
  }

  def getPropertyRepresentationsForAgent(agentId: String) = Action.async { implicit request =>
    reprConnector.forAgent(agentId) flatMap { reps =>
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

  def create() = Action.async { implicit request =>
    val reprRequest: PropertyRepresentation = request.body.asJson.get.as[PropertyRepresentation]
    reprConnector.create(reprRequest).map(x => Ok(""))
  }

  def update() = Action.async { implicit request =>
    val reprRequest: PropertyRepresentation = request.body.asJson.get.as[PropertyRepresentation]
    reprConnector.update(reprRequest).map(x => Ok(""))
  }

  def accept(reprId: String) = Action.async { implicit request =>
    reprConnector.accept(reprId).map(x => Ok(""))
  }

  def reject(reprId: String) = Action.async { implicit request =>
    reprConnector.reject(reprId).map(x => Ok(""))
  }

}
