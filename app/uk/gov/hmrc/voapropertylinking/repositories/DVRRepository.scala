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

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Singleton}
import javax.inject.Inject
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.Mdc

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DVRRepository @Inject()(
      mongo: ReactiveMongoComponent,
      @Named("dvrCollectionName") val dvrCollectionName: String,
      config: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends ReactiveRepository[DVRRecord, String](
      dvrCollectionName,
      mongo.mongoConnector.db,
      DVRRecord.mongoFormat,
      implicitly[Format[String]]) with DVRRecordRepository {

  lazy val ttlDuration = config.getDuration("dvr.record.ttl.duration")

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("createdAt" -> IndexType.Ascending),
      name = Some("ttl"),
      options = BSONDocument("expireAfterSeconds" -> ttlDuration.toSeconds))
  )

  override def create(request: DetailedValuationRequest): Future[Unit] =
    Mdc.preservingMdc {
      insert(DVRRecord(request.organisationId, request.assessmentRef, request.agents))
        .map(_ => ())
        .recover {
          case e: DatabaseException => Logger.debug(e.getMessage())
        }
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
}

case class DVRRecord(
      organisationId: Long,
      assessmentRef: Long,
      agents: Option[List[Long]]
)

object DVRRecord {

  import reactivemongo.play.json.BSONFormats.BSONDateTimeFormat

  val mongoFormat: OFormat[DVRRecord] = new OFormat[DVRRecord] {
    override def writes(o: DVRRecord): JsObject =
      Json.writes[DVRRecord].writes(o) ++ Json.obj("createdAt" -> BSONDateTime(System.currentTimeMillis))

    override def reads(json: JsValue): JsResult[DVRRecord] = Json.reads[DVRRecord].reads(json)
  }
}

@ImplementedBy(classOf[DVRRepository])
trait DVRRecordRepository {
  def create(dvr: DetailedValuationRequest): Future[Unit]

  def exists(organisationId: Long, assessmentRef: Long): Future[Boolean]

  def clear(organisationId: Long): Future[Unit]
}
