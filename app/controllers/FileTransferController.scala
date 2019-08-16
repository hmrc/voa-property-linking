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
import models.mdtp.fileupload.{Available, Callback}
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import services.FileTransferService

import scala.concurrent.{ExecutionContext, Future}

class FileTransferController @Inject()(
                                        val fileTransferService: FileTransferService
                                      )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  def handleCallback(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    Logger.info(request.body.validate[Callback].toString)
    withJsonBody[Callback] {
      case Callback(envelopeId, _, Available, _)  =>
        fileTransferService
          .transferManually(envelopeId)
          .map { _ =>
            Ok
          }
      case Callback(envelopeId, _, status, _)     =>
        Logger.info(s"Received callback for $envelopeId; File status: $status")
        Future.successful(Ok)
    }
  }

  def run(): Action[AnyContent] = Action.async { implicit request =>
    fileTransferService.justDoIt().map(_ => Ok("File transfer was started manually"))
  }
}
