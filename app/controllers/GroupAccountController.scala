/*
 * Copyright 2018 HM Revenue & Customs
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
import auditing.AuditingService
import connectors.{BusinessRatesAuthConnector, GroupAccountConnector}
import models.{GroupAccountSubmission, GroupId, UpdatedOrganisationAccount}
import play.api.libs.json.Json
import play.api.mvc.Action

class GroupAccountController @Inject() (
                                       val auth: AuthConnector,
                                         groups: GroupAccountConnector, brAuth: BusinessRatesAuthConnector)
  extends PropertyLinkingBaseController with Authenticated {

  case class GroupAccount(groupId: GroupId, submission: GroupAccountSubmission)

  object GroupAccount {
    implicit val format = Json.format[GroupAccount]
  }
  def get(organisationId: Long) = authenticated { implicit request =>
    groups.get(organisationId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def withGroupId(groupId: String) = authenticated { implicit request =>
    groups.findByGGID(groupId) map {
      case Some(x) => Ok(Json.toJson(x))
      case None => NotFound
    }
  }

  def withAgentCode(agentCode: String) = authenticated { implicit request =>
    groups.withAgentCode(agentCode) map {
      case Some(a) => Ok(Json.toJson(a))
      case None => NotFound
    }
  }

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[GroupAccountSubmission] { acc =>
      groups.create(acc) map { x =>
        AuditingService.sendEvent("Created", GroupAccount(x, acc))
        Created(Json.toJson(x)) }
    }
  }

  def update(orgId: Long) = authenticated(parse.json) { implicit request =>
    withJsonBody[UpdatedOrganisationAccount] { acc =>
      for {
        _ <- groups.update(orgId, acc)
        _ <- brAuth.clearCache()
      } yield Ok
    }
  }
}
