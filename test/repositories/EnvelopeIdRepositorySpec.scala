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

import basespecs.BaseUnitSpec
import models.{Closed, EnvelopeStatus, Open}
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDateTime
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global

class EnvelopeIdRepositorySpec
  extends BaseUnitSpec
    with MongoSpecSupport {

  val app = appBuilder.build()

  val repository = new EnvelopeIdRepository(app.injector.instanceOf[ReactiveMongoComponent], this.getClass.getSimpleName) {
    override def indexes: Seq[Index] = Seq.empty
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}")

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.drop.futureValue
    repository.ensureIndexes.futureValue
  }

  private val envelopeId = "envId1234"
  private val status = Open

  "repository" should {
    "have an empty index initially" in {
      repository.indexes shouldBe Seq.empty
    }
  }

  "repository" should {
    "insert an envelopeId in the repository with the provided status" in {
      repository.create(envelopeId, status).futureValue shouldBe (())
    }
  }

  "repository" should {
    "get an empty list of envelopeIds from the repository" in {
      repository.get().futureValue shouldBe Seq.empty
    }
  }

  "repository" should {
    "get all the envelopeIds from the repository" in {
      val result = Seq(EnvelopeId(
        envelopeId = envelopeId,
        _id = envelopeId,
        status = Some(status),
        createdAt = Some(BSONDateTime(System.currentTimeMillis))))
      repository.create(envelopeId, status).futureValue
      val pickImportantBits: EnvelopeId => (String, Option[EnvelopeStatus]) =
        eid => eid.envelopeId -> eid.status
      repository.get().futureValue.map(pickImportantBits) shouldBe result.map(pickImportantBits)
    }
  }

  "repository" should {
    "get return the status open for the provided envelopeId" in {
      repository.create(envelopeId, status).futureValue
      repository.getStatus(envelopeId).futureValue shouldBe Some(Open)
    }
  }

  "repository" should {
    "get return None for an envelopeId that doesn't exist" in {
      repository.getStatus(envelopeId).futureValue shouldBe None
    }
  }

  "repository" should {
    "update the envelopeId with the status Closed " in {
      repository.create(envelopeId, status).futureValue
      repository.update(envelopeId, Closed).futureValue shouldBe (())
      repository.getStatus(envelopeId).futureValue shouldBe Some(Closed)
    }
  }

  "repository" should {
    "not update the status for an envelopeId that does not exist" in {
      repository.update(envelopeId, Closed).futureValue shouldBe (())
    }
  }

  "repository" should {
    "delete the envelope for and id that exists" in {
      repository.create(envelopeId, status).futureValue
      repository.delete(envelopeId).futureValue shouldBe (())
    }
  }

  "repository" should {
    "handle delete for an envelope id that does not exist" in {
      repository.delete(envelopeId).futureValue shouldBe (())
    }
  }

}

