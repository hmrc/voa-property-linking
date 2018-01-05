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

package modules

import java.util.Base64

import com.google.inject.name.{Named, Names}
import com.google.inject.{AbstractModule, Inject, Singleton}
import org.joda.time.Duration
import play.api.libs.json.{JsObject, Json}
import play.api.{Configuration, Environment, Logger}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.{DB, ReadPreference}
import reactivemongo.json._
import reactivemongo.json.collection.JSONCollection
import uk.gov.hmrc.lock.{ExclusiveTimePeriodLock, LockMongoRepository, LockRepository}

class AdHocQuery(environment: Environment, configuration: Configuration) extends AbstractModule {
  def configure(): Unit = {
    if (configuration.getString("adhoc.mongo.enabled").getOrElse("false").toBoolean) {
      bindConstant().annotatedWith(Names.named("collection")).to(configuration.getString("adhoc.mongo.collection")
        .getOrElse(throw new RuntimeException("Missing property adhoc.mongo.collection")))

      val unencodedQuery = new String(Base64.getDecoder.decode(configuration.getString("adhoc.mongo.query")
        .getOrElse(throw new RuntimeException("Missing property adhoc.mongo.query"))), "UTF-8")

      bindConstant().annotatedWith(Names.named("query")).to(unencodedQuery)
      bind(classOf[AdhocQueryRunner]).to(classOf[AdhocQueryRunnerImpl]).asEagerSingleton()
    }
  }
}

trait AdhocQueryRunner extends ExclusiveTimePeriodLock {
  val db: () => DB
  val logger = Logger("AdhocQuery")

  override val lockId: String = "AdhocQueryLock"
  override val holdLockFor: Duration = Duration.standardMinutes(10)
  override def repo: LockRepository = LockMongoRepository(db)
}

@Singleton
class AdhocQueryRunnerImpl @Inject() (reactiveMongoComponent: ReactiveMongoComponent,
                                      @Named("collection") collection: String,
                                      @Named("query") query: String) extends AdhocQueryRunner {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val db = reactiveMongoComponent.mongoConnector.db

  tryToAcquireOrRenewLock {
    logger.info(s"Adhoc query running: db.getCollection('$collection').find($query)")

    val col = db().collection[JSONCollection](collection)
    val results = col.find(Json.parse(query).as[JsObject]).cursor[JsObject](ReadPreference.primary, false).collect[List]()

    results.map {
      case Nil => logger.info("No results found")
      case l =>
        logger.info(s"${l.length} items found")
        logger.info(s"${Json.obj("results" -> l)}")
    }.andThen { case _ => logger.info("Adhoc query processing: end") }
  }
}
