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

package modules.tasks

import javax.inject.{Inject, Singleton}

import models.Closed
import modules.MongoTask
import play.api.Environment
import repositories.{EnvelopeId, EnvelopeIdRepo}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddTimestamps @Inject()(val env: Environment, envelopeRepo: EnvelopeIdRepo)(implicit ec: ExecutionContext) extends MongoTask[EnvelopeId] {
  override val upToVersion = 1

  override def verify: (String) => Option[EnvelopeId] = ???

  override def execute: (EnvelopeId) => Future[Unit] = ???

  override def run(version: Int): Future[Unit] = {
    envelopeRepo.get() flatMap { envs =>
      Future.sequence(envs.map { env => envelopeRepo.update(env.envelopeId, env.status.getOrElse(Closed))} )
    } map { _ => () }
  }
}
