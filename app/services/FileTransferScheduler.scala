/*
 * Copyright 2016 HM Revenue & Customs
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
import infrastructure.{Lock, LockedJobScheduler, Schedule}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class FileTransferScheduler @Inject()(val lock: Lock,
                                      val transerService: FileTransferService,
                                      val actorSystem: ActorSystem,
                                      @Named("regularSchedule") val schedule: Schedule)
  extends LockedJobScheduler[FileTransferComplete](lock, actorSystem)
{
  val name = "FileTransferer"
  this.start()
  implicit val hc = new HeaderCarrier()
  override def runJob()(implicit ec: ExecutionContext) = transerService.justDoIt.map(_ => FileTransferComplete(""))
}


case class FileTransferComplete(msg: String)
