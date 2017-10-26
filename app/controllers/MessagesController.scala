/*
 * Copyright 2017 HM Revenue & Customs
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
import connectors.MessagesConnector
import connectors.auth.AuthConnector
import models.messages.MessageSearchParams
import play.api.libs.json.Json
import play.api.mvc.Action

class MessagesController @Inject()(val auth: AuthConnector,
                                   messagesConnector: MessagesConnector) extends PropertyLinkingBaseController with Authenticated {

  //TODO authenticated endpoints
  def getMessages(params: MessageSearchParams) = Action.async { implicit request =>
    messagesConnector.getMessages(params) map { msgs => Ok(Json.toJson(msgs)) }
  }

  def messageCountFor(organisationId: Long) = Action.async { implicit request =>
    messagesConnector.getMessageCount(organisationId) map { c => Ok(Json.toJson(c)) }
  }

  def readMessage(messageId: String, readBy: String) = Action.async { implicit request =>
    messagesConnector.readMessage(messageId, readBy) map { _ => Ok }
  }
}
