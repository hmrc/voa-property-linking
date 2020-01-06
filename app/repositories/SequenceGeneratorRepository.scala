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

package repositories

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONString}
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONBatchCommands.FindAndModifyCommand
import uk.gov.hmrc.mongo.{ReactiveRepository, Repository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Sequence(_id: String, sequence: Long)

object Sequence {
  val format: OFormat[Sequence] = Json.format
}

trait SequenceGeneratorRepository extends Repository[Sequence, String] {
  def getNextSequenceId(key: String): Future[Long]
}

@Singleton
class SequenceGeneratorMongoRepository @Inject()(mongo: ReactiveMongoComponent)
  extends ReactiveRepository[Sequence, String](
    collectionName = "sequences",
    mongo = mongo.mongoConnector.db,
    domainFormat = Json.format[Sequence],
    idFormat = implicitly[Format[String]]
  ) with SequenceGeneratorRepository {

  def sequenceNo(in: FindAndModifyCommand.FindAndModifyResult): Long =
    in.result[Sequence].map(_.sequence).getOrElse(throw new RuntimeException("Unable to generate sequence number"))

  override def getNextSequenceId(key: String): Future[Long] = {
    // get latest from sequence, if none - then set next number to 600_000_000, else inc by one
    // if num=999_999_999 throw error
    val selector: BSONDocument = BSONDocument("_id" -> BSONString(key))

    def update(inc: Int): BSONDocument = BSONDocument(s"$$inc" -> BSONDocument("sequence" -> BSONInteger(inc)))

    findLatestSequence(key).flatMap { optSequence =>
      optSequence.fold(
        collection.findAndUpdate(selector, update(600000000), fetchNewObject = true, upsert = true).map(sequenceNo)
      ) {
        case sequence if sequence.sequence == 999999999 =>
          Future.failed(new RuntimeException("Reached upper limit of 999999999 on generating sequence number"))
        case _ =>
          collection.findAndUpdate(selector, update(1), fetchNewObject = true, upsert = false).map(sequenceNo)
      }
    }
  }

  private def findLatestSequence(key: String): Future[Option[Sequence]] =
    collection.find(Json.obj("_id" -> key)).sort(Json.obj("_id" -> -1)).one[Sequence]

}
