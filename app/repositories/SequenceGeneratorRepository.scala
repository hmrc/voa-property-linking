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

package repositories

import javax.inject.Inject

import com.google.inject.Singleton
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONString}
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}
import reactivemongo.json.ImplicitBSONHandlers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Sequence(_id: String, sequence: Long)

object Sequence {
  val format = Json.format[Sequence]
}

trait SequenceGeneratorRepository extends Repository[Sequence, String] {

  def getNextSequenceId(key: String): Future[Long]
}

@Singleton
class SequenceGeneratorMongoRepository @Inject()(db: DB) extends
  ReactiveRepository[Sequence, String]("sequences", () => db, Json.format[Sequence], implicitly[Format[String]])
  with SequenceGeneratorRepository {


  override def getNextSequenceId(key: String): Future[Long] = {
    // get latest from sequence, if none - then set next number to 600_000_000, else inc by one
    // if num=999_999_999 throw error
    findLatestSequence(key).flatMap {

      case Some(sequence) if sequence.sequence == 999999999 =>
        throw new IllegalStateException("Reached upper limit of 999999999 on generating sequence number")

      case Some(sequence) =>
        collection.findAndUpdate(
          BSONDocument("_id" -> BSONString(key)),
          BSONDocument("$inc" -> BSONDocument("sequence" -> BSONInteger(1))),
          fetchNewObject = true,
          upsert = true).map(
          _.result[Sequence].map(Json.toJson(_).\("sequence").as[Long]).getOrElse(throw new IllegalStateException("Unable to generate sequence number"))
        )

      case None =>
        collection.findAndUpdate(
          BSONDocument("_id" -> BSONString(key)),
          BSONDocument("$inc" -> BSONDocument("sequence" -> BSONInteger(600000000))),
          fetchNewObject = true,
          upsert = true).map(
          _.result[Sequence].map(Json.toJson(_).\("sequence").as[Long]).getOrElse(throw new IllegalStateException("Unable to generate sequence number"))
        )
    }
  }

  private def findLatestSequence(key: String): Future[Option[Sequence]] =
    collection
      .find(Json.obj("_id" -> key))
      .sort(Json.obj("_id" -> -1))
      .one[Sequence]
}
