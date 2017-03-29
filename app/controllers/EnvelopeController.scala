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

import com.google.inject.Singleton
import models.Closed
import play.api.mvc.Action
import repositories.EnvelopeIdRepository
import services.FileTransferScheduler

@Singleton
class EnvelopeController @Inject()(val repo: EnvelopeIdRepository, val service: FileTransferScheduler)
  extends PropertyLinkingBaseController {

  def create(envelopeId: String) = Action.async { implicit request =>
    repo.create(envelopeId).map(_=> Ok(envelopeId))
  }

  def close(envelopeId: String) = Action.async { implicit request =>
    repo.update(envelopeId, Closed).map(_=> Ok(envelopeId))
  }

  def get() = Action.async { implicit  request =>
    repo.get().map(seq => Ok(seq.mkString("\n")))
  }

}
