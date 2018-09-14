/*
 * Copyright 2018 HM Revenue & Customs
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

import models.{Closed, Open}
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDateTime
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class EnvelopeIdRepositorySpec
  extends UnitSpec
    with BeforeAndAfterEach
    with MongoSpecSupport
{
  val repository = new EnvelopeIdRepository(mongo(), s"${this.getClass.getSimpleName}") {
    override def indexes: Seq[Index] = Seq.empty
  }

  protected def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}")

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  private val envelopeId = "envId1234"
  private val status = Open

  "repository" should {
    "have an empty index initially" in {
      await(repository.indexes shouldBe Seq.empty)
    }
  }

  "repository" should {
    "insert an envelopeId in the repository with the provided status" in {
      await(repository.create(envelopeId, status)) shouldBe ()
    }
  }

  "repository" should {
    "get an empty list of envelopeIds from the repository" in {
      await(repository.get()) shouldBe Seq.empty
    }
  }

  "repository" should {
    "get all the envelopeIds from the repository" in {
      val result = Seq(EnvelopeId(envelopeId, envelopeId, Some(status), Some(BSONDateTime(System.currentTimeMillis))))
      await(repository.create(envelopeId, status))
      await(repository.get()) shouldBe result
    }
  }

  "repository" should {
    "get return the status open for the provided envelopeId" in {
      await(repository.create(envelopeId, status))
      await(repository.getStatus(envelopeId)) shouldBe Some(Open)
    }
  }

  "repository" should {
    "get return None for an envelopeId that doesn't exist" in {
      await(repository.getStatus(envelopeId)) shouldBe None
    }
  }

  "repository" should {
    "update the envelopeId with the status Closed " in {
      await(repository.create(envelopeId, status))
      await(repository.update(envelopeId, Closed)) shouldBe ()
      await(repository.getStatus(envelopeId)) shouldBe Some(Closed)
    }
  }

  "repository" should {
    "not update the status for an envelopeId that does not exist" in {
      await(repository.update(envelopeId, Closed)) shouldBe ()
    }
  }

  "repository" should {
    "delete the envelope for and id that exists" in {
      await(repository.create(envelopeId, status))
      await(repository.delete(envelopeId)) shouldBe ()
    }
  }

  "repository" should {
    "handle delete for an envelope id that does not exist" in {
      await(repository.delete(envelopeId)) shouldBe ()
    }
  }

}

