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

package infrastructure

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Scheduler}
import akka.event.EventStream
import akka.util.Timeout
import play.api.Logger
import uk.gov.hmrc.lock.LockKeeper

abstract class LockedJobScheduler[Event <: AnyRef](lock: LockKeeper, actorSystem: ActorSystem) {
  implicit val t: Timeout = 1 hour

  val name: String
  val schedule: Schedule
  val scheduler: Scheduler = actorSystem.scheduler
  val eventStream = actorSystem.eventStream
  def runJob()(implicit ec: ExecutionContext): Future[Event]

	def start()(implicit ec: ExecutionContext) { 
    scheduleNextImport() 
  }

  private def run()(implicit ec: ExecutionContext) = {
    Logger.info(s"Starting job: $name")
    runJob().map { event => 
      eventStream.publish(event)
    } recoverWith {
      case e: Exception =>
        Logger.error(s"Error running job: $name", e)
        Future.failed(e)
    }
  }

  private def scheduleNextImport()(implicit ec: ExecutionContext) {
    val t = schedule.timeUntilNextRun
    Logger.info(s"Scheduling $name to run in: $t")
  	scheduler.scheduleOnce(t) {
      lock.tryLock { run } onComplete { _ => scheduleNextImport() }
    }
  }
}
