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
import auth.Authenticated
import connectors.auth.DefaultAuthConnector
import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import javax.inject.Inject
import models._
import play.api.Logger
import play.api.libs.json.Json

class PropertyRepresentationController @Inject() (
                                                   val authConnector: DefaultAuthConnector,
                                                   representations: PropertyRepresentationConnector,
                                                   propertyLinksConnector: PropertyLinkingConnector,
                                                   groupAccountsConnector: GroupAccountConnector,
                                                   auditingService: AuditingService
                                                 ) extends PropertyLinkingBaseController with Authenticated {

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
        Ok("")
      }
    }
  }

  def response() = authenticated(parse.json) { implicit request =>
    withJsonBody[APIRepresentationResponse] { r =>
      representations.response(r) map { _ =>
        auditingService.sendEvent("agent representation response", r)
        Ok("") }
    }
  }

   def revoke(authorisedPartyId: Long) =  authenticated(parse.json) { implicit request =>

     representations.revoke(authorisedPartyId)  map { _ =>
       Ok("")
     }
   }

  def appointableToAgent(ownerId: Long,
                         agentCode: Long,
                         checkPermission: Option[String],
                         challengePermission: Option[String],
                         paginationParams: PaginationParams,
                         sortfield: Option[String],
                         sortorder: Option[String],
                         address: Option[String],
                         agent: Option[String]) = authenticated { implicit request =>

    groupAccountsConnector.withAgentCode(agentCode.toString) flatMap {
      case Some(agentGroup) => propertyLinksConnector.appointableToAgent(
        ownerId = ownerId,
        agentId = agentGroup.id,
        checkPermission = checkPermission,
        challengePermission = challengePermission,
        params = paginationParams,
        sortfield = sortfield,
        sortorder = sortorder,
        address = address,
        agent = agent).map(x => Ok(Json.toJson(x)))
      case None =>
        Logger.error(s"Agent details lookup failed for agentCode: $agentCode")
        NotFound
    }
  }
}
