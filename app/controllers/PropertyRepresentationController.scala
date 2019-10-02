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
import models._
import models.mdtp.propertylink.projections.OwnerAuthResult
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.connectors.modernised._

import scala.concurrent.{ExecutionContext, Future}

class PropertyRepresentationController @Inject()(
                                                  authenticated: AuthenticatedActionBuilder,
                                                  authorisationManagementApi: AuthorisationManagementApi,
                                                  authorisationSearchApi: AuthorisationSearchApi,
                                                  customerManagementApi: CustomerManagementApi,
                                                  auditingService: AuditingService
                                                )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[RepresentationRequest] { reprRequest =>
      authorisationManagementApi
        .create(APIRepresentationRequest.fromRepresentationRequest(reprRequest))
        .map(_ => Ok(""))
    }
  }

  def validateAgentCode(
                         agentCode: Long,
                         authorisationId: Long
                       ): Action[AnyContent] = authenticated.async { implicit request =>
    authorisationManagementApi
      .validateAgentCode(agentCode, authorisationId)
      .map { errorOrOrganisationId =>
        errorOrOrganisationId.fold(
          orgId => Ok(Json.obj("organisationId" -> orgId)),
          errorString => Ok(Json.obj("failureCode" -> errorString)))
      }
  }

  def forAgent(
                status: String,
                organisationId: Long,
                pagination: PaginationParams
              ): Action[AnyContent] = authenticated.async { implicit request =>
    authorisationSearchApi
      .forAgent(status, organisationId, pagination)
      .map { propertyRepresentation =>
        Ok(Json.toJson(propertyRepresentation))
      }
  }

  def response(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[APIRepresentationResponse] { representationResponse =>
      authorisationManagementApi
        .response(representationResponse)
        .map { _ =>
          auditingService.sendEvent("agent representation response", representationResponse)
          Ok("")
        }
    }
  }

  def revoke(authorisedPartyId: Long): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    authorisationManagementApi
      .revoke(authorisedPartyId)
      .map(_ => Ok(""))
  }

  def appointableToAgent(
                          ownerId: Long,
                          agentCode: Long,
                          checkPermission: Option[String],
                          challengePermission: Option[String],
                          paginationParams: PaginationParams,
                          sortfield: Option[String],
                          sortorder: Option[String],
                          address: Option[String],
                          agent: Option[String]
                        ): Action[AnyContent] = authenticated.async { implicit request =>
    customerManagementApi
      .withAgentCode(agentCode.toString)
      .flatMap {
        case Some(agentGroup) =>
          authorisationSearchApi.appointableToAgent(
            ownerId = ownerId,
            agentId = agentGroup.id,
            checkPermission = checkPermission,
            challengePermission = challengePermission,
            params = paginationParams,
            sortfield = sortfield,
            sortorder = sortorder,
            address = address,
            agent = agent
          )
            .map(x => Ok(Json.toJson(OwnerAuthResult(x))))
        case None =>
          Logger.error(s"Agent details lookup failed for agentCode: $agentCode")
          Future.successful(NotFound)
      }
  }
}
