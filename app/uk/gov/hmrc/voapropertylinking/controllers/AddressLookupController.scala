/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import cats.data.OptionT

import javax.inject.Inject
import models.modernised.addressmanagement.SimpleAddress
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.AddressManagementApi
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ModernisedAddressManagementApi
import uk.gov.hmrc.voapropertylinking.utils.PostcodeValidator

import scala.concurrent.{ExecutionContext, Future}

class AddressLookupController @Inject() (
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      modernisedAddresses: ModernisedAddressManagementApi,
      addresses: AddressManagementApi,
      featureSwitch: FeatureSwitch
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def find(postcode: String): Action[AnyContent] =
    authenticated.async { implicit request =>
      PostcodeValidator.validateAndFormat(postcode) match {
        case Some(s) =>
          if (featureSwitch.isBstDownstreamEnabled)
            addresses.find(s).map(r => Ok(Json.toJson(r)))
          else
            modernisedAddresses.find(s).map(r => Ok(Json.toJson(r)))
        case None => Future.successful(BadRequest)
      }
    }

  def get(addressUnitId: Long): Action[AnyContent] =
    authenticated.async { implicit request =>
      OptionT {
        if (featureSwitch.isBstDownstreamEnabled)
          addresses.get(addressUnitId)
        else
          modernisedAddresses.get(addressUnitId)
      }.fold[Result](NotFound)(a => Ok(Json.toJson(a)))
    }

  def create: Action[JsValue] =
    authenticated.async(parse.json) { implicit request =>
      withJsonBody[SimpleAddress] { address =>
        if (featureSwitch.isBstDownstreamEnabled)
          addresses.create(address).map(id => Created(Json.obj("id" -> id)))
        else
          modernisedAddresses.create(address).map(id => Created(Json.obj("id" -> id)))
      }
    }
}
