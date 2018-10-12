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
import connectors.MessagesConnector
import connectors.auth.{AuthConnector, DefaultAuthConnector}
import models.messages.{MessageSearchParams, MessageSearchResults}
import play.api.libs.json.Json

class MessagesController @Inject()(val authConnector: DefaultAuthConnector,
                                   messagesConnector: MessagesConnector) extends PropertyLinkingBaseController with Authenticated {

  def getMessage(recipientOrganisationId: Long, messageId: String) = authenticated { implicit request =>
    messagesConnector.getMessage(recipientOrganisationId, messageId) map {
      case MessageSearchResults(_, _, m :: _) => Ok(Json.toJson(m))
      case _ => NotFound
    }
  }

  def getMessages(params: MessageSearchParams) = authenticated { implicit request =>
    messagesConnector.getMessages(params) map { msgs => Ok(Json.toJson(msgs)) }
  }

  def messageCountFor(organisationId: Long) = authenticated { implicit request =>
    messagesConnector.getMessageCount(organisationId) map { c => Ok(Json.toJson(c)) }
  }

  def readMessage(messageId: String, readBy: String) = authenticated { implicit request =>
    messagesConnector.readMessage(messageId, readBy) map { _ => Ok }
  }
}
