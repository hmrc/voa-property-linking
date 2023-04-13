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

import uk.gov.hmrc.voapropertylinking.auditing.AuditingService

import javax.inject.Inject
import models.{GroupAccountSubmission, GroupId, UpdatedOrganisationAccount}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.CustomerManagementApi
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised.ModernisedCustomerManagementApi

import scala.concurrent.{ExecutionContext, Future}

class GroupAccountController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      auditingService: AuditingService,
      modernisedCustomerManagementApi: ModernisedCustomerManagementApi,
      customerManagementApi: CustomerManagementApi,
      featureSwitch: FeatureSwitch,
      brAuth: BusinessRatesAuthConnector
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  case class GroupAccount(groupId: GroupId, submission: GroupAccountSubmission)

  object GroupAccount {
    implicit val format = Json.format[GroupAccount]
  }

  def get(organisationId: Long): Action[AnyContent] = authenticated.async { implicit request =>
    val optGroupAccount: Future[Option[models.GroupAccount]] =
      if (featureSwitch.isBstDownstreamEnabled) {
        customerManagementApi
          .getDetailedGroupAccount(organisationId)
      } else {
        modernisedCustomerManagementApi
          .getDetailedGroupAccount(organisationId)
      }
    optGroupAccount.map {
      case Some(x) => Ok(Json.toJson(x))
      case None    => NotFound
    }
  }

  def withGroupId(groupId: String): Action[AnyContent] = authenticated.async { implicit request =>
    val optGroupAccount: Future[Option[models.GroupAccount]] =
      if (featureSwitch.isBstDownstreamEnabled) {
        customerManagementApi
          .findDetailedGroupAccountByGGID(groupId)
      } else {
        modernisedCustomerManagementApi
          .findDetailedGroupAccountByGGID(groupId)
      }
    optGroupAccount.map {
      case Some(x) => Ok(Json.toJson(x))
      case None    => NotFound
    }
  }

  def withAgentCode(agentCode: String): Action[AnyContent] = authenticated.async { implicit request =>
    val optGroupAccount: Future[Option[models.GroupAccount]] =
      if (featureSwitch.isBstDownstreamEnabled) {
        customerManagementApi.withAgentCode(agentCode)
      } else {
        modernisedCustomerManagementApi.withAgentCode(agentCode)
      }
    optGroupAccount.map {
      case Some(a) => Ok(Json.toJson(a))
      case None    => NotFound
    }
  }

  def create(): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[GroupAccountSubmission] { acc =>
      val createGroupAccountResponse: Future[GroupId] =
        if (featureSwitch.isBstDownstreamEnabled) {
          customerManagementApi.createGroupAccount(acc)
        } else {
          modernisedCustomerManagementApi.createGroupAccount(acc)
        }
      createGroupAccountResponse.map { groupId =>
        auditingService.sendEvent("Created", GroupAccount(groupId, acc))
        Created(Json.toJson(groupId))
      }
    }
  }

  def update(orgId: Long): Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[UpdatedOrganisationAccount] { acc =>
      for {
        _ <- if (featureSwitch.isBstDownstreamEnabled) {
              customerManagementApi.updateGroupAccount(orgId, acc)
            } else {
              modernisedCustomerManagementApi.updateGroupAccount(orgId, acc)
            }
        _ <- brAuth.clearCache()
      } yield Ok
    }
  }
}
