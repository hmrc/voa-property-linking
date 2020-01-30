/*
 * Copyright 2020 HM Revenue & Customs
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

import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import javax.inject.Inject
import models.{IndividualAccountId, IndividualAccountSubmission}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.EmptyContent
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised.CustomerManagementApi

import scala.concurrent.ExecutionContext

class IndividualAccountController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      customerManagementApi: CustomerManagementApi,
      auditingService: AuditingService,
      brAuth: BusinessRatesAuthConnector
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  case class IndividualAccount(id: IndividualAccountId, submission: IndividualAccountSubmission)

  object IndividualAccount {
    implicit val format = Json.format[IndividualAccount]
  }

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[IndividualAccountSubmission] { acc =>
      customerManagementApi
        .createIndividualAccount(acc)
        .map { personId =>
          auditingService.sendEvent("Created", IndividualAccount(personId, acc))
          Created(Json.toJson(personId))
        }
    }
  }

  def update(personId: Long): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[IndividualAccountSubmission] { account =>
      for {
        _ <- customerManagementApi.updateIndividualAccount(personId, account)
        _ <- brAuth.clearCache()
      } yield {
        Ok(EmptyContent())
      }
    }
  }

  def get(personId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    customerManagementApi
      .getDetailedIndividual(personId)
      .map {
        case Some(x) => Ok(Json.toJson(x))
        case None    => NotFound
      }
  }

  def withExternalId(externalId: String): Action[AnyContent] = authenticated.async { implicit request =>
    customerManagementApi
      .findDetailedIndividualAccountByGGID(externalId)
      .map {
        case Some(x) => Ok(Json.toJson(x))
        case None    => NotFound
      }
  }

}
