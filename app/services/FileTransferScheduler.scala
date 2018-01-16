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

package services

import javax.inject.{Inject, Named}

import akka.actor.ActorSystem
import com.google.inject.Singleton
import infrastructure.{Lock, LockedJobScheduler, Schedule}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait FileTransfer

@Singleton
class FileTransferScheduler @Inject()(val lock: Lock,
                                      val transferService: FileTransferService,
                                      val actorSystem: ActorSystem,
                                      @Named("regularSchedule") val schedule: Schedule)
  extends LockedJobScheduler[FileTransferComplete](lock, actorSystem) with FileTransfer {

  val name = "FileTransferer"

  implicit val hc = new HeaderCarrier()

  override def runJob()(implicit ec: ExecutionContext): Future[FileTransferComplete] = transferService.justDoIt

  Logger.info("Starting file transfer scheduler")
  start
}

@Singleton
class FileTransferDisabled extends FileTransfer {
  Logger.info("File transfer scheduler disabled")
}

case class FileTransferComplete(msg: Option[String] = None)
