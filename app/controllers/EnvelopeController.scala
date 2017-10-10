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
import connectors.auth.AuthConnector
import connectors.fileUpload.{EnvelopeMetadata, FileUploadConnector}
import models.Closed
import play.api.libs.json.Json
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException

class EnvelopeController @Inject()(val auth: AuthConnector,
                                   val repo: EnvelopeIdRepo, fileUploadConnector: FileUploadConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def create = authenticated(parse.json) { implicit request =>
    withJsonBody[EnvelopeMetadata] { metadata =>
      fileUploadConnector.createEnvelope(metadata, routes.FileTransferController.handleCallback().absoluteURL()) flatMap {
        case Some(id) => repo.create(id) map { _ => Ok(Json.obj("envelopeId" -> id))}
        case None => InternalServerError(Json.obj("error" -> "envelope creation failed"))
      } recover {
        case _: UnhealthyServiceException => ServiceUnavailable(Json.obj("error" -> "file upload service not available"))
      }
    }
  }

  // temporarily kept for backwards compatibility
  def record(envelopeId: String) = authenticated { implicit request =>
    repo.create(envelopeId).map(_=> Ok(envelopeId))
  }

  def close(envelopeId: String) = authenticated { implicit request =>
    repo.update(envelopeId, Closed).map(_=> Ok(envelopeId))
  }
}
