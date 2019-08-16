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

import auditing.AuditingService
import connectors.{BusinessRatesAuthConnector, GroupAccountConnector}
import javax.inject.Inject
import models.{GroupAccountSubmission, GroupId, UpdatedOrganisationAccount}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.voa.voapropertylinking.actions.AuthenticatedActionBuilder

import scala.concurrent.ExecutionContext

class GroupAccountController @Inject() (
                                         authenticated: AuthenticatedActionBuilder,
                                         auditingService: AuditingService,
                                         groups: GroupAccountConnector, brAuth: BusinessRatesAuthConnector
                                       )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  case class GroupAccount(groupId: GroupId, submission: GroupAccountSubmission)

  object GroupAccount {
    implicit val format = Json.format[GroupAccount]
  }

  def get(organisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    groups
      .get(organisationId)
      .map {
        case Some(x)  => Ok(Json.toJson(x))
        case None     => NotFound
    }
  }

  def withGroupId(groupId: String): Action[AnyContent] = authenticated.async { implicit request =>
    groups
      .findByGGID(groupId)
      .map {
        case Some(x)  => Ok(Json.toJson(x))
        case None     => NotFound
      }
  }

  def withAgentCode(agentCode: String): Action[AnyContent] = authenticated.async { implicit request =>
    groups
      .withAgentCode(agentCode)
      .map {
        case Some(a)  => Ok(Json.toJson(a))
        case None     => NotFound
      }
  }

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[GroupAccountSubmission] { acc =>
      groups
        .create(acc)
        .map { groupId =>
          auditingService.sendEvent("Created", GroupAccount(groupId, acc))
          Created(Json.toJson(groupId))
        }
    }
  }

  def update(orgId: Long): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[UpdatedOrganisationAccount] { acc =>
      for {
        _ <- groups.update(orgId, acc)
        _ <- brAuth.clearCache()
      } yield Ok
    }
  }
}
