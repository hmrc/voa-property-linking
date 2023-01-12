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

import com.google.inject.Singleton
import javax.inject.Inject
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts._
import org.mongodb.scala.model.Updates._
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.play.http.logging.Mdc
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions, ReturnDocument}

import scala.concurrent.{ExecutionContext, Future}

case class Sequence(_id: String, sequence: Long)

object Sequence {
  implicit val format: Format[Sequence] = Json.format
}

trait SequenceGeneratorRepository extends PlayMongoRepository[Sequence] {
  def getNextSequenceId(key: String): Future[Long]
}

@Singleton
class SequenceGeneratorMongoRepository @Inject()(mongo: MongoComponent)(implicit executionContext: ExecutionContext)
    extends PlayMongoRepository[Sequence](
      collectionName = "sequences",
      mongoComponent = mongo,
      domainFormat = implicitly,
      indexes = Seq(IndexModel(ascending("sequence"), IndexOptions().sparse(false).unique(true)))
    ) with SequenceGeneratorRepository {

  override def getNextSequenceId(key: String): Future[Long] =
    // get latest from sequence, if none - then set next number to 600_000_000, else inc by one
    // if num=999_999_999 throw error
    Mdc.preservingMdc {
      findLatestSequence(key).flatMap {
        case Some(sequence) if sequence.sequence >= 999999999 =>
          throw new IllegalStateException("Reached upper limit of id on generating sequence number")
        case Some(_) => update(key = key, value = 1)
        case None    => update(key = key, value = 600000000)
      }
    }

  private def update(key: String, value: Int): Future[Long] =
    collection
      .findOneAndUpdate(
        equal("_id", key),
        inc("sequence", value),
        options = FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER)
      )
      .toFuture()
      .map(_.sequence)

  private def findLatestSequence(key: String): Future[Option[Sequence]] =
    collection.find(equal("_id", key)).toFuture().map(_.headOption)

}
