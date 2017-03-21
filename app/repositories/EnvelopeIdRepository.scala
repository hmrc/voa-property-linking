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

import com.google.inject.{ImplementedBy, Singleton}
import play.api.Logger
import play.api.libs.json._
import reactivemongo.api.DB
import reactivemongo.core.errors.DatabaseException
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EnvelopeIdRepository @Inject()(db: DB)
  extends ReactiveRepository[EnvelopeId, String]("envelopeids", () => db, EnvelopeId.mongoFormat, implicitly[Format[String]]) with
    EnvelopeIdRepo {

  override def create(envelopeId: String): Future[Unit] = {
    insert(EnvelopeId(envelopeId, envelopeId))
      .map(_ => ())
      .recover {
        case e: DatabaseException if e.code.contains(11000) =>
          Logger.debug(s"EnvelopeId: $envelopeId has already been added")
      }
  }

  override def get(): Future[Seq[String]] = {
    findAll().map(x =>
      x.map(_.envelopeId))
  }

  override def remove(envelopeId: String) = {
    Logger.info(s"Deleting envelopedId: $envelopeId from mongo")
    removeById(envelopeId).map(_ => ())
  }
}

case class EnvelopeId(envelopeId: String, _id: String)

object EnvelopeId {
  val mongoFormat = Json.format[EnvelopeId]
}

@ImplementedBy(classOf[EnvelopeIdRepository])
trait EnvelopeIdRepo {
  def create(envelopeId: String): Future[Unit]

  def get(): Future[Seq[String]]

  def remove(envelopeId: String)
}
