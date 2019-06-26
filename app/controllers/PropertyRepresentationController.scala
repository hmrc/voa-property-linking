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
import auditing.AuditingService
import auth.Authenticated
import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import connectors.auth.DefaultAuthConnector
import models._
import models.searchApi.AgentAuthResultFE
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Action

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


  /** *
    * Make two calls to the Search/Sort API
    * the first call returns the results based on supplied filters and sortfield
    * the second call is used only to allow us to get the count of PENDING representation requests
    */
  def forAgentSearchAndSort(organisationId: Long,
                            paginationParams: PaginationParams,
                            sortfield: Option[String],
                            sortorder: Option[String],
                            status: Option[String],
                            address: Option[String],
                            baref: Option[String],
                            client: Option[String]) = authenticated { implicit request =>
    val eventualAuthResultBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      sortfield = sortfield,
      sortorder = sortorder,
      status = status,
      address = address,
      baref = baref,
      client = client,
      representationStatus = Some("APPROVED")) // TODO cater for other statuses

    // required to calculate the pending count - no filtering/sorting required
    val eventualAuthResultPendingBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      representationStatus = Some("PENDING"))

    // required to get the correct filtered amount - no filtering/sorting required
    val eventualAuthResultApprovedNoFiltersBE = propertyLinksConnector.agentSearchAndSort(
      organisationId = organisationId,
      params = paginationParams,
      representationStatus = Some("APPROVED"))

    for {
      authResultBE <- eventualAuthResultBE
      authResultPendingBE <- eventualAuthResultPendingBE
      authResultApprovedNoFiltersBE <- eventualAuthResultApprovedNoFiltersBE
    } yield Ok(Json.toJson(AgentAuthResultFE(authResultBE, authResultPendingBE.filterTotal, authResultApprovedNoFiltersBE.filterTotal)))

  }

}
