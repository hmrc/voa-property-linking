/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Singleton}
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import org.bson.BsonType
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Projections
import org.mongodb.scala.model.Updates.set
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.localDateTimeFormat
import uk.gov.hmrc.play.http.logging.Mdc
import uk.gov.hmrc.voapropertylinking.services.DateTimeService

import java.time.LocalDateTime
import javax.inject.Inject
import scala.compat.Platform.currentTime
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DVRRepository @Inject()(
      mongo: MongoComponent,
      dateTimeService: DateTimeService,
      @Named("dvrCollectionName") val dvrCollectionName: String)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[DVRRecord](
      collectionName = dvrCollectionName,
      mongoComponent = mongo,
      domainFormat = DVRRecord.mongoFormat,
      indexes = Seq.empty
    ) with DVRRecordRepository with Logging {

  override def create(request: DetailedValuationRequest): Future[Unit] =
    Mdc.preservingMdc {
      collection
        .insertOne(
          DVRRecord(
            request.organisationId,
            request.assessmentRef,
            request.agents,
            Some(request.submissionId),
            dateTimeService.now()))
        .toFuture()
        .map(_ => ())
        .recover {
          case e: Exception => logger.debug(e.getMessage())
        }
    }

  override def find(organisationId: Long, assessmentRef: Long): Future[Option[DVRRecord]] =
    Mdc.preservingMdc {
      collection
        .find(and(query(organisationId), equal("assessmentRef", assessmentRef)))
        .headOption()
    }

  override def clear(organisationId: Long): Future[Unit] =
    Mdc.preservingMdc {
      collection
        .findOneAndDelete(query(organisationId))
        .toFuture()
        .map(_ => ())
        .recover {
          case e: Exception => logger.debug(e.getMessage())
        }
    }

  private def query(organisationId: Long): Bson =
    or(equal("organisationId", organisationId), in("agents", Seq(organisationId)))

  override def findDocumentsNoTimestamp: Future[Seq[String]] = {
    val longReads: Reads[String] = (__ \ "_id").read[String]
    collection
      .find[BsonValue](or(exists("createdAt", false), `type`("createdAt", BsonType.INT32)))
      .projection(Projections.include("_id"))
      .limit(1000)
      .toFuture()
      .map(_.map(bson => Codecs.fromBson[String](bson)(longReads)))
  }

  override def updateCreatedAtTimestampById(ids: Seq[String]): Future[Long] =
    collection
      .updateMany(filter = in("_id", ids: _*), update = set("createdAt", dateTimeService.now()))
      .toFuture()
      .map(_.getModifiedCount)

}

case class DVRRecord(
      organisationId: Long,
      assessmentRef: Long,
      agents: Option[List[Long]],
      dvrSubmissionId: Option[String],
      createdAtTimestamp: LocalDateTime
)

object DVRRecord {
  implicit val dateFormat = localDateTimeFormat
  implicit val mongoFormat: Format[DVRRecord] = Json.format[DVRRecord]
}

@ImplementedBy(classOf[DVRRepository])
trait DVRRecordRepository {
  def create(dvr: DetailedValuationRequest): Future[Unit]

  def find(organisationId: Long, assessmentRef: Long): Future[Option[DVRRecord]]

  def clear(organisationId: Long): Future[Unit]

  def findDocumentsNoTimestamp: Future[Seq[String]]

  def updateCreatedAtTimestampById(ids: Seq[String]): Future[Long]
}
