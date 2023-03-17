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

package uk.gov.hmrc.voapropertylinking.repositories.helpers

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Filters.in
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, MongoSupport}
import uk.gov.hmrc.voapropertylinking.repositories.DVRRecord

import scala.concurrent.ExecutionContext

trait MongoSpec extends AnyWordSpec with MongoSupport with GuiceOneAppPerSuite
  with CleanMongoCollectionSupport with Matchers {
  implicit val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  import scala.language.implicitConversions

  implicit class MongoOps(repo: PlayMongoRepository[DVRRecord])(implicit ec: ExecutionContext) {
    def removeAll: DeleteResult = await(repo.collection.deleteMany(BsonDocument()).toFuture())
    def awaitCount: Int = await(repo.collection.countDocuments().toFuture()).toInt
    def awaitInsert(dvr: DVRRecord): InsertOneResult = await(repo.collection.insertOne(dvr).toFuture())
    def updateCreatedAtWrongDataType(orgId: Long): UpdateResult = await(repo.collection.updateOne(in("organisationId", orgId),
      set("createdAt", 1)).toFuture())
  }
}
