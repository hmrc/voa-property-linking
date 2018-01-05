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

import auditing.AuditingService
import auth.Authenticated
import connectors.PropertyRepresentationConnector
import connectors.auth.AuthConnector
import models._
import play.api.libs.json.Json
import play.api.mvc.Action

class PropertyRepresentationController @Inject() (val auth: AuthConnector,
                                                  representations: PropertyRepresentationConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def validateAgentCode(agentCode:Long, authorisationId: Long) = authenticated { implicit request =>
    representations.validateAgentCode(agentCode, authorisationId).map(
      _.fold(
        orgId => Ok(Json.obj("organisationId" -> orgId)),
        errorString => Ok(Json.obj("failureCode" -> errorString))
      )
    )
  }

  def forAgent(status: String, organisationId: Long, pagination: PaginationParams) = authenticated { implicit request =>
    representations.forAgent(status, organisationId, pagination).map( x=> Ok(Json.toJson(x)))
  }

  def create() = authenticated(parse.json) { implicit request =>
    withJsonBody[RepresentationRequest] { reprRequest =>
      representations.create(APIRepresentationRequest.fromRepresentationRequest(reprRequest)).map{ x =>
        AuditingService.sendEvent("create agent request success", reprRequest)
        Ok("")
      }
    }
  }

  def response() = authenticated(parse.json) { implicit request =>
    withJsonBody[APIRepresentationResponse] { r =>
      representations.response(r) map { _ =>
        AuditingService.sendEvent("agent response success", r)
        Ok("") }
    }
  }

  def revoke(authorisedPartyId: Long) = authenticated { implicit request =>
    representations.revoke(authorisedPartyId)  map { _ =>
      AuditingService.sendEvent("agent revoke success", Map("authorisedPartyId" -> authorisedPartyId))
      Ok("")
    }
  }

}
