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

package infrastructure

import javax.inject.{Inject, Named}

import com.google.inject.Singleton
import org.joda.time.{DateTimeZone, Duration}
import play.api.Logger
import reactivemongo.api.DB
import uk.gov.hmrc.lock.{LockKeeper, LockMongoRepository, LockRepository}
import uk.gov.hmrc.mongo.CurrentTime

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class Lock @Inject()(@Named("lockName") val name: String, @Named("lockTimeout") val timeout: Duration, val db: DB) extends LockKeeper with CurrentTime {
  override def repo: LockRepository = LockMongoRepository(() => db)

  override def lockId: String = name

  override val forceLockReleaseAfter: Duration = timeout

  override def tryLock[T](body: => Future[T])(implicit ec : ExecutionContext): Future[Option[T]] = {
    repo.lock(lockId, serverId, forceLockReleaseAfter)
      .flatMap {
        case true => lockAcquired(body)
        case _ => locked
      }.recoverWith { case ex => repo.releaseLock(lockId, serverId).flatMap(_ => Future.failed(ex)) }
  }

  def lockAcquired[T](body: Future[T])(implicit ec: ExecutionContext): Future[Option[T]] = {
    withCurrentTime { now =>
      Logger.info(s"Acquired lock - setting release to: ${now.plus(timeout).withZone(zone)}")
      body.map(x => Some(x))
    }
  }

  def locked[T]: Future[Option[T]] = {
    Logger.info("Lock not acquired - already held")
    Future.successful(None)
  }
}
