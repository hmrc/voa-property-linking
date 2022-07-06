/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.repositories

import java.time.Instant

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Singleton}
import javax.inject.Inject
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import org.mongodb.scala.bson.conversions.Bson
import play.api.libs.json._
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Filters._

import scala.concurrent.duration._
import org.mongodb.scala.model.Indexes.ascending
import play.api.Logging
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DVRRepository @Inject()(
      mongo: MongoComponent,
      @Named("dvrCollectionName") val dvrCollectionName: String,
      config: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[DVRRecord](
      collectionName = dvrCollectionName,
      mongoComponent = mongo,
      domainFormat = DVRRecord.mongoFormat,
      indexes = Seq(
        IndexModel(
          ascending("createdAt"),
          IndexOptions().name("ttl").expireAfter(config.getDuration("dvr.record.ttl.duration").toSeconds, SECONDS)))
    ) with DVRRecordRepository with Logging {

  override def create(request: DetailedValuationRequest): Future[Unit] =
    Mdc.preservingMdc {
      collection
        .insertOne(DVRRecord(request.organisationId, request.assessmentRef, request.agents))
        .toFuture()
        .map(_ => ())
        .recover {
          case e: DatabaseException => logger.debug(e.getMessage())
        }
    }

  override def exists(organisationId: Long, assessmentRef: Long): Future[Boolean] =
    Mdc.preservingMdc {
      collection.find(or(query(organisationId))).toFuture().map(_.exists(_.assessmentRef == assessmentRef))
    }

  override def clear(organisationId: Long): Future[Unit] =
    Mdc.preservingMdc {
      collection
        .findOneAndDelete(query(organisationId))
        .toFuture()
        .map(_ => ())
        .recover {
          case e: DatabaseException => logger.debug(e.getMessage())
        }
    }

  private def query(organisationId: Long): Bson =
    or(equal("organisationId", organisationId), in("agents", Seq(organisationId)))

}

case class DVRRecord(
      organisationId: Long,
      assessmentRef: Long,
      agents: Option[List[Long]]
)

object DVRRecord {

  private implicit val dateFormat = uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat

  val mongoFormat: OFormat[DVRRecord] = new OFormat[DVRRecord] {
    override def writes(o: DVRRecord): JsObject =
      Json.writes[DVRRecord].writes(o) ++ Json.obj("createdAt" -> Instant.now())

    override def reads(json: JsValue): JsResult[DVRRecord] = Json.reads[DVRRecord].reads(json)
  }
}

@ImplementedBy(classOf[DVRRepository])
trait DVRRecordRepository {
  def create(dvr: DetailedValuationRequest): Future[Unit]

  def exists(organisationId: Long, assessmentRef: Long): Future[Boolean]

  def clear(organisationId: Long): Future[Unit]
}
