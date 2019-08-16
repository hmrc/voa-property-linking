/*
 * Copyright 2019 HM Revenue & Customs
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
import models.SimpleAddress
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.AddressManagementApi
import uk.gov.hmrc.voapropertylinking.utils.PostcodeValidator

import scala.concurrent.{ExecutionContext, Future}

class AddressLookup @Inject()(
                               authenticated: AuthenticatedActionBuilder,
                               addresses: AddressManagementApi
                             )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  def find(postcode: String): Action[AnyContent] = authenticated.async { implicit request =>
    PostcodeValidator.validateAndFormat(postcode) match {
      case Some(s)  => addresses.find(s).map(r => Ok(Json.toJson(r)))
      case None     => Future.successful(BadRequest)
    }
  }

  def get(addressUnitId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    addresses.get(addressUnitId) map {
      case Some(a)  => Ok(Json.toJson(a))
      case None     => NotFound
    }
  }

  def create: Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[SimpleAddress] { address =>
      addresses
        .create(address)
        .map { id =>
          Created(Json.obj("id" -> id))
        }
    }
  }
}
