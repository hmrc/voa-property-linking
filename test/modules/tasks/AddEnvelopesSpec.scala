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

package modules.tasks

import java.util.UUID

import models.Closed
import modules.tasks.AddEnvelopes
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Environment
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONDateTime
import repositories.{EnvelopeId, EnvelopeIdRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class AddEnvelopesSpec extends UnitSpec
  with BeforeAndAfterEach
  with MongoSpecSupport
  with MockitoSugar
  with WithFakeApplication{

  val repository = new EnvelopeIdRepository(mongo(), s"${this.getClass.getSimpleName}") {
    override def indexes: Seq[Index] = Seq.empty
  }

  val mockEnv = mock[Environment]
  val unitUnderTest = new AddEnvelopes(mockEnv, repository)
  val timeNow = Some(BSONDateTime(System.currentTimeMillis))
  val uuid = UUID.randomUUID().toString
  val envId = EnvelopeId(uuid, uuid, Some(Closed), timeNow)

  "execute" should {
    "insert the line into the repo" in {
      val function1 = unitUnderTest.execute
      function1(envId) onSuccess {case r => r} shouldBe ()
    }
  }

  "verify" should {
    "return the envelopeId for the provided valid uuid" in {
      val function1: String => Option[EnvelopeId] = unitUnderTest.verify
      val result = function1(uuid).map(e => (e._id, e.envelopeId, e.status))
      result.get shouldBe (envId._id, envId.envelopeId, envId.status)
    }
  }

  "verify" should {
    "return None for an invalid uuid" in {
      val function1: String => Option[EnvelopeId] = unitUnderTest.verify
      val result = function1("not a valid uuid")
      result shouldBe None
    }
  }

}
