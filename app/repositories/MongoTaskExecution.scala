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

import java.time.LocalDateTime
import javax.inject.Inject

import com.google.inject.Singleton
import play.api.libs.json._
import reactivemongo.api.DB
import uk.gov.hmrc.mongo.ReactiveRepository

@Singleton
class MongoTaskExecution @Inject()(db: DB)
  extends ReactiveRepository[MongoTaskRegister, String]("mongoTaskExecution", () => db, MongoTaskRegister.mongoFormat, implicitly[Format[String]]) {
}

case class MongoTaskRegister(taskName: String, version: Int, executionDateTime: LocalDateTime)

object MongoTaskRegister {
  val mongoFormat = Json.format[MongoTaskRegister]
}