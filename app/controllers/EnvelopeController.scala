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

import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import models.Closed
import play.api.libs.json.Json
import play.api.mvc.Action
import repositories.EnvelopeIdRepo

class EnvelopeController @Inject()(val repo: EnvelopeIdRepo, fileUploadConnector: FileUploadConnector) extends PropertyLinkingBaseController {

  def create = Action.async(parse.json) { implicit request =>
    withJsonBody[EnvelopeMetadata] { metadata =>
      fileUploadConnector.createEnvelope(metadata) flatMap {
        case Some(id) => repo.create(id) map { _ => Ok(Json.obj("envelopeId" -> id))}
        case None => InternalServerError(Json.obj("error" -> "envelope creation failed"))
      }
    }
  }

  // temporarily kept for backwards compatibility
  def record(envelopeId: String) = Action.async { implicit request =>
    repo.create(envelopeId).map(_=> Ok(envelopeId))
  }

  def close(envelopeId: String) = Action.async { implicit request =>
    repo.update(envelopeId, Closed).map(_=> Ok(envelopeId))
  }
}
