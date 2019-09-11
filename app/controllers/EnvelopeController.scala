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
import models.EnvelopeStatus
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.{EnvelopeMetadata, FileUploadConnector}

import scala.concurrent.{ExecutionContext, Future}

class EnvelopeController @Inject()(
                                    authenticated: AuthenticatedActionBuilder,
                                    val repo: EnvelopeIdRepo,
                                    fileUploadConnector: FileUploadConnector,
                                    config: ServicesConfig
                                  )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  lazy val mdtpPlatformSsl = config.getBoolean("mdtp.platformSsl")

  def create: Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[EnvelopeMetadata] { metadata =>
      fileUploadConnector
        .createEnvelope(metadata, routes.FileTransferController.handleCallback().absoluteURL(mdtpPlatformSsl))
        .flatMap {
          case Some(id) =>
            repo.create(id).map(_ => Ok(Json.obj("envelopeId" -> id)))
          case None => Future.successful(InternalServerError(Json.obj("error" -> "envelope creation failed")))
        } recover {
        case _: UnhealthyServiceException => ServiceUnavailable(Json.obj("error" -> "file upload service not available"))
      }
    }
  }

  def record(envelopeId: String): Action[AnyContent] = authenticated.async { implicit request =>
    repo
      .create(envelopeId)
      .map { _ =>
        Ok(envelopeId)
      }
  }

  def close(envelopeId: String): Action[AnyContent] = authenticated.async { implicit request =>
    repo.update(envelopeId, EnvelopeStatus.CLOSED).map(_ => Ok(envelopeId))
  }
}
