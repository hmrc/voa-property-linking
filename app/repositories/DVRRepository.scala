/*
 * Copyright 2019 HM Revenue & Customs
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

package repositories

import com.google.inject.name.Named
import com.google.inject.{ImplementedBy, Singleton}
import javax.inject.Inject
import play.api.Logger
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

@Singleton
class DVRRepository @Inject()(db: DB, @Named("dvrCollectionName") val dvrCollectionName: String)
  extends ReactiveRepository[DVRRecord, String](dvrCollectionName, () => db, DVRRecord.mongoFormat, implicitly[Format[String]]) with
    DVRRecordRepository with ServicesConfig {

  lazy val ttlDuration = getDuration("dvr.record.ttl.duration")

  override def indexes: Seq[Index] = Seq(
    Index(key = Seq("createdAt" -> IndexType.Ascending), name = Some("ttl"), options = BSONDocument("expireAfterSeconds" -> (ttlDuration).toSeconds))
  )

  override def create(organisationId: Long, assessmentRef: Long): Future[Unit] = {
    insert(DVRRecord(organisationId, assessmentRef, now))
      .map(_ => ())
      .recover {
        case e: DatabaseException => Logger.debug(e.getMessage())
      }
  }

  override def exists(organisationId: Long, assessmentRef: Long): Future[Boolean] = {
    find("organisationId" -> organisationId).map(dvrRecords =>
      dvrRecords.find(dvrRecord => dvrRecord.assessmentRef == assessmentRef)).map {
      case Some(dvrRecord) => true
      case None => false
    }
  }

  override def clear(organisationId: Long): Future[Unit] = {
    remove("organisationId" -> organisationId)
      .map(_ => ())
      .recover {
        case e: DatabaseException => Logger.debug(e.getMessage())
      }
  }

  private def now = Some(BSONDateTime(System.currentTimeMillis))
}

case class DVRRecord(organisationId: Long, assessmentRef: Long, createdAt: Option[BSONDateTime])

object DVRRecord {

  import reactivemongo.json.BSONFormats.BSONDateTimeFormat

  val mongoFormat = Json.format[DVRRecord]
}

@ImplementedBy(classOf[DVRRepository])
trait DVRRecordRepository {
  def create(organisationId: Long, assessmentRef: Long): Future[Unit]

  def exists(organisationId: Long, assessmentRef: Long): Future[Boolean]

  def clear(organisationId: Long): Future[Unit]
}


