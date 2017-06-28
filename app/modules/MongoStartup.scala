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
import modules.tasks.{AddEnvelopes, RemoveEnvelopes}
import org.joda.time.Duration
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import repositories.{MongoTaskExecution, MongoTaskRegister}
import uk.gov.hmrc.lock.{ExclusiveTimePeriodLock, LockMongoRepository, LockRepository}

import scala.collection.JavaConversions
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class MongoStartup(environment: Environment, configuration: Configuration) extends AbstractModule {
  def configure(): Unit = {
    val mb = Multibinder.newSetBinder(binder(), new TypeLiteral[MongoTask[_]]() {})
    mb.addBinding().to(classOf[RemoveEnvelopes])
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
    logger.info(s"MongoStartup: running")

    tasks.foldLeft(Future.successful(())) { (prev, task) =>
      prev.map { _ =>
        logger.info(s"Processing ${task.name}...")

        for (version <- 1 to task.version) {
          mongoTaskRepo.find("taskName" -> task.name, "version" -> version).map {
            case Nil =>
              logger.info(s"Task ${task.name} version $version not yet executed - running")
              mongoTaskRepo.insert(MongoTaskRegister(task.name, version, LocalDateTime.now))
              task.run()
            case head :: Nil => alreadyRun(task)(head)
            case head :: _ => alreadyRun(task)(head)
          }
        }
      }
    }.map { _ => logger.info("MongoStartup: end") }
  }

  def alreadyRun(task: MongoTask[_]): MongoTaskRegister => Future[Unit] = { head =>
    logger.info(s"Mongo task ${task.name} version ${task.version} already ran at ${head.executionDateTime}")
    Future.successful(())
  }
}

trait MongoTask[T] {
  val env: Environment
  val name: String = this.getClass.getSimpleName
  val version: Int
  lazy val srcFile = s"$name-$version.txt"

  def verify: String => Option[T]
  def execute: T => Future[Unit]

  def run(): Future[Unit] = {
    Logger.info(s"Loading from file: $srcFile")
    Source.fromInputStream(env.classLoader.getResourceAsStream(s"tasks/$srcFile")).getLines.foldLeft(Future.successful(())) { (prev, line) =>
      prev.map { _ =>
        verify(line) match {
          case Some(data) =>
            Logger.info(s"""Verification of $name - "$line" successful""")
            execute(data)
          case None =>
            Logger.warn(s"""Verification of $name - "$line" failed - skipping""")
            Future.successful(())
        }
      }
    }
  }
}
