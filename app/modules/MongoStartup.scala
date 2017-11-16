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

package modules

import java.time.LocalDateTime

import com.google.inject.multibindings.Multibinder
import com.google.inject.{AbstractModule, Inject, Singleton, TypeLiteral}
import modules.tasks.{AddEnvelopes, AddTimestamps, RemoveEnvelopes}
import org.joda.time.Duration
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import repositories.{MongoTaskExecution, MongoTaskRegister}
import uk.gov.hmrc.lock.{ExclusiveTimePeriodLock, LockMongoRepository, LockRepository}

import scala.collection.JavaConversions
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class MongoStartup(environment: Environment, configuration: Configuration) extends AbstractModule {
  def configure(): Unit = {
    val mb = Multibinder.newSetBinder(binder(), new TypeLiteral[MongoTask[_]]() {})
    mb.addBinding().to(classOf[RemoveEnvelopes])
    mb.addBinding().to(classOf[AddTimestamps])
    mb.addBinding().to(classOf[AddEnvelopes])

    bind(classOf[MongoStartupRunner]).to(classOf[MongoStartupRunnerImpl]).asEagerSingleton()
  }
}

trait MongoStartupRunner extends ExclusiveTimePeriodLock {
  val db: () => DB
  val logger = Logger("MongoStartup")

  override val lockId: String = "MongoStartupLock"
  override val holdLockFor: Duration = Duration.standardMinutes(10)
  override def repo: LockRepository = LockMongoRepository(db)
}

@Singleton
class MongoStartupRunnerImpl @Inject() (reactiveMongoComponent: ReactiveMongoComponent,
                                        mongoTaskRepo: MongoTaskExecution,
                                        tasks: java.util.Set[MongoTask[_]]) extends MongoStartupRunner {
  import JavaConversions._

  override val db = reactiveMongoComponent.mongoConnector.db

  tryToAcquireOrRenewLock {
    tasks.foldLeft(Future(logger.info(s"MongoStartup: running"))) { (prev, task) =>
      prev.flatMap { _ =>
        (1 to task.upToVersion).foldLeft(Future(logger.info(s"Processing ${task.name}..."))) { (prev, version) =>
          prev.flatMap { _ =>
            mongoTaskRepo.find("taskName" -> task.name, "version" -> version).flatMap {
              case Nil => Future(logger.info(s"Task ${task.name} version $version not yet executed - running"))
                .map(_ => mongoTaskRepo.insert(MongoTaskRegister(task.name, version, LocalDateTime.now)))
                .flatMap(_ => task.run(version))
              case head :: Nil => alreadyRun(task, version)(head)
              case head :: _ => alreadyRun(task, version)(head)
            }
          }
        }
      }
    }.map { _ => logger.info("MongoStartup: end") }
  }

  def alreadyRun(task: MongoTask[_], version: Int): MongoTaskRegister => Future[Unit] = { head =>
    Future(logger.info(s"Mongo task ${task.name} version $version already ran at ${head.executionDateTime}"))
  }
}

trait MongoTask[T] {
  val env: Environment
  val name: String = this.getClass.getSimpleName
  val upToVersion: Int

  def verify: String => Option[T]
  def execute: T => Future[Unit]

  def run(version: Int): Future[Unit] = {
    val srcFile = s"$name-$version.txt"

    Source.fromInputStream(env.classLoader.getResourceAsStream(s"tasks/$srcFile")).getLines
      .foldLeft(Future(Logger.info(s"Loading from file: $srcFile"))) { (prev, line) =>
      prev.flatMap { _ =>
        verify(line) match {
          case Some(data) => Future(Logger.info(s"""Verification of $name - "$line" successful""")).flatMap(_ => execute(data))
          case None => Future(Logger.warn(s"""Verification of $name - "$line" failed - skipping"""))
        }
      }
    }
  }
}
