/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument, BSONObjectID}
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats.objectIdFormats
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class DVRRepository @Inject()(
                               mongo: ReactiveMongoComponent,
                               @Named("dvrCollectionName") val dvrCollectionName: String,
                               config: ServicesConfig
                             )(implicit executionContext: ExecutionContext)
  extends ReactiveRepository[DVRRecord, BSONObjectID](
    dvrCollectionName,
    mongo.mongoConnector.db,
    DVRRecord.mongoFormat,
    implicitly[Format[BSONObjectID]]) with DVRRecordRepository {

  private val loggur = Logger(this.getClass.getName)

  lazy val ttlDuration = config.getDuration("dvr.record.ttl.duration")

  import reactivemongo.play.json._

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("createdAt" -> IndexType.Ascending),
      name = Some("ttl"),
      options = BSONDocument("expireAfterSeconds" -> (ttlDuration).toSeconds))
  )

  override def create(request: DetailedValuationRequest): Future[Unit] =
    Mdc.preservingMdc {
      insert(DVRRecord(request.organisationId, request.assessmentRef, request.agents))
        .map(_ => ())
        .recover {
          case e: DatabaseException => Logger.debug(e.getMessage())
        }
    }

  // TODO remove after conversion
  override def findAll(): Future[List[DVRRecord]] =
    Mdc.preservingMdc(find())

  // TODO remove after conversion
  override def update(dvrRecord: DVRRecord) = {
    Mdc.preservingMdc(
      collection.update(ordered = true).one(BSONDocument("_id" -> dvrRecord._id.get), dvrRecord, upsert = false))

    Future.successful(dvrRecord)
  }

  override def exists(organisationId: Long, assessmentRef: Long): Future[Boolean] =
    Mdc.preservingMdc {
      find(query(organisationId))
        .map(_.exists(_.assessmentRef == assessmentRef))
    }

  override def clear(organisationId: Long): Future[Unit] =
    Mdc.preservingMdc {
      remove(query(organisationId))
        .map(_ => ())
        .recover {
          case e: DatabaseException => Logger.debug(e.getMessage())
        }
    }

  private def query(organisationId: Long): (String, Json.JsValueWrapper) =
    "$or" -> Json.arr(Json.obj("organisationId" -> organisationId), Json.obj("agents" -> organisationId))

  {
    loggur.info("** DVR-conv - start *****")
    loggur.info(s"** DVR-conv - ttlDuration: ${ttlDuration.toSeconds}")

    def log(dvr: DVRRecord) =
      loggur.info(s"** DVR-conv ** $dvr : ${Instant.ofEpochMilli(dvr.createdAt.value)}")

    Await.result( for {
      before <- findAll
      _ = before.foreach(log)
      updated <- Future.traverse(before)(update)
      _ = loggur.info("** DVR-conv converted")
      _ = updated.foreach(log)
    } yield (updated.size), Duration.Inf)

    loggur.info("** DVR-conv - end *****")
  }
}

case class DVRRecord(
                      organisationId: Long,
                      assessmentRef: Long,
                      agents: Option[List[Long]],
                      _id: Option[BSONObjectID] = None, // TODO remove later
                      createdAt: BSONDateTime = BSONDateTime(System.currentTimeMillis()) // TODO remove after conversion
                    )

object DVRRecord {
  import reactivemongo.play.json.BSONFormats.BSONDateTimeFormat

  implicit val mongoFormat: OFormat[DVRRecord] = new OFormat[DVRRecord] {
    override def writes(o: DVRRecord): JsObject =
      Json.writes[DVRRecord].writes(o) //TODO ++ Json.obj("createdAt" -> System.currentTimeMillis())

    def readsCreatedAt: Reads[BSONDateTime] = new Reads[BSONDateTime] {
      override def reads(json: JsValue): JsResult[BSONDateTime] =
        json.validate[BSONDateTime] match {
          case value @ JsSuccess(_, _) => value
          case JsError(errors) =>
            json.validate[Long] match {
              case JsSuccess(value, path) => JsSuccess(BSONDateTime(value))
              case JsError(errors)        => JsSuccess(BSONDateTime(System.currentTimeMillis()))
            }
        }
    }

    // TODO override def reads(json: JsValue): JsResult[DVRRecord] = Json.reads[DVRRecord].reads(json)
    // TODO remove after conversion
    override def reads(json: JsValue): JsResult[DVRRecord] =
      (
        (__ \ "_id").read[BSONObjectID] and
          (__ \ "organisationId").read[Long] and
          (__ \ "assessmentRef").read[Long] and
          (__ \ "agents").readNullable[List[Long]] and
          (__ \ "createdAt").read[BSONDateTime](readsCreatedAt)
        )(mkDvrRecord _).reads(json)

    private def mkDvrRecord(
                             _id: BSONObjectID,
                             organisationId: Long,
                             assessmentRef: Long,
                             agents: Option[List[Long]],
                             createdAt: BSONDateTime
                           ) =
      DVRRecord(
        _id = Some(_id),
        organisationId = organisationId,
        assessmentRef = assessmentRef,
        agents = agents,
        createdAt = createdAt)
  }

}

@ImplementedBy(classOf[DVRRepository])
trait DVRRecordRepository {
  def create(dvr: DetailedValuationRequest): Future[Unit]

  def exists(organisationId: Long, assessmentRef: Long): Future[Boolean]

  def clear(organisationId: Long): Future[Unit]

  // TODO remove after conversion
  def findAll(): Future[List[DVRRecord]]
  def update(dvr: DVRRecord): Future[DVRRecord]
}
