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

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import models.Closed
import modules.MongoTask
import play.api.{Environment, Logger}
import play.api.libs.concurrent.Execution.Implicits._
import reactivemongo.bson.BSONDateTime
import repositories.{EnvelopeId, EnvelopeIdRepository}

import scala.concurrent.Future
import scala.util.Try

@Singleton
class AddEnvelopes @Inject()(override val env: Environment, envelopeRepo: EnvelopeIdRepository) extends MongoTask[EnvelopeId] {
  override val upToVersion: Int = 6
  override def verify: String => Option[EnvelopeId] = line => Try(UUID.fromString(line)).map(_ => Some(EnvelopeId(line, line, Some(Closed), Some(BSONDateTime(System.currentTimeMillis))))).getOrElse(None)
  override def execute: EnvelopeId => Future[Unit] = line => envelopeRepo.insert(line)
    .map(_ => Logger.info(s"Added: $line"))
    .recover{ case _ => Logger.info(s"Already exists: $line")}
}
