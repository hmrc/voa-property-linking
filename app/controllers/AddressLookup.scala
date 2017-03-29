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

import com.google.inject.Singleton
import connectors.AddressConnector
import models.SimpleAddress
import play.api.libs.json.Json
import play.api.mvc.Action
import util.PostcodeValidator

import scala.concurrent.Future

@Singleton
class AddressLookup @Inject() (addresses: AddressConnector) extends PropertyLinkingBaseController {

  def find(postcode: String) = Action.async { implicit request =>
    PostcodeValidator.validateAndFormat(postcode) match {
      case Some(s) => addresses.find(s).map(r => Ok(Json.toJson(r)))
      case None => Future.successful(BadRequest)
    }
  }

  def get(addressUnitId: Int) = Action.async { implicit request =>
    addresses.get(addressUnitId) map {
      case Some(a) => Ok(Json.toJson(a))
      case None => NotFound
    }
  }

  def create = Action.async(parse.json) { implicit request =>
    withJsonBody[SimpleAddress] { address =>
      addresses.create(address) map { x => Created(Json.obj("id" -> x)) }
    }
  }
}
