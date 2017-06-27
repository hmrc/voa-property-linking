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

package modules.tasks

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import modules.MongoTask
import play.api.Environment
import repositories.EnvelopeIdRepo

import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class RemoveEnvelopes @Inject() (override val env: Environment, envelopeRepo: EnvelopeIdRepo) extends MongoTask {
  override val version: Int = 1
  override def verify: String => Boolean = line => Try(UUID.fromString(line)).isSuccess
  override def execute: String => Future[Unit] = id => envelopeRepo.remove(id).map(_ => s"Deleted $id")
}
