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

import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import reactivemongo.api.DB
import reactivemongo.bson.{BSONDateTime, BSONDocument}
import uk.gov.hmrc.mongo.ReactiveRepository
import reactivemongo.json.BSONFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import reactivemongo.api.indexes.{Index, IndexType}
import scala.concurrent.duration._

import scala.concurrent.Future

trait LoginAttemptsRepo {
  def mostRecent(ipAddress: String, amount: Int, since: DateTime): Future[Seq[FailedLogin]]
  def recordFailure(ip: String): Future[Unit]
}

class LoginAttemptsMongoRepo(mongo: () => DB) extends ReactiveRepository[LoginHistory, String]("logins", mongo, LoginHistory.format, implicitly[Format[String]]) with LoginAttemptsRepo {

  override def mostRecent(ipAddress: String, amount: Int, since: DateTime): Future[Seq[FailedLogin]] = {
    findById(ipAddress) map {
      case Some(logins) => logins.attempts.sortBy(_.value).reverse.take(amount).map(toFailedLogin(_, ipAddress)).filter(_.timestamp.isAfter(since.minusSeconds(1)))
      case None => Seq.empty
    }
  }

  private def toFailedLogin(dt: BSONDateTime, ip: String) = FailedLogin(new DateTime(dt.value), ip)

  override def recordFailure(ip: String): Future[Unit] = {
    val modifier = BSONDocument(
      ("$push", BSONDocument(("attempts", BSONDateTime(System.currentTimeMillis()))))
    )
    upsert(ip, modifier).map(_ => Unit)
  }

  private def upsert(ip: String, modifier: BSONDocument) = {
    collection.findAndUpdate(BSONDocument(("_id", ip)), modifier, fetchNewObject = false, upsert = true)
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq(("attempts",  IndexType.Ascending)),
      name = Some("failedLoginsTTL"),
      options = BSONDocument("expireAfterSeconds" -> (7 days).toSeconds)
    )
  )
}

case class LoginHistory(_id: String, attempts: Seq[BSONDateTime])

object LoginHistory {
  val format = Json.format[LoginHistory]
}

case class FailedLogin(timestamp: DateTime, ipAddress: String)
