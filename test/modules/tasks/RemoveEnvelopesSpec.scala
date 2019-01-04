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

package modules.tasks

import java.util.UUID

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Environment
import reactivemongo.api.indexes.Index
import repositories.EnvelopeIdRepository
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RemoveEnvelopesSpec extends UnitSpec
  with BeforeAndAfterEach
  with MongoSpecSupport
  with MockitoSugar
  with WithFakeApplication{

  val repository = new EnvelopeIdRepository(mongo(), s"${this.getClass.getSimpleName}") {
    override def indexes: Seq[Index] = Seq.empty
  }

  val mockEnv = mock[Environment]
  val unitUnderTest = new RemoveEnvelopes(mockEnv, repository)
  val uuid = UUID.randomUUID().toString

  "execute" should {
    "delete the id from the repo" in {
      val function1: String => Future[Unit] = unitUnderTest.execute
      function1(uuid) onSuccess {case r => r} shouldBe ()
    }
  }

  "verify" should {
    "extract the valid uuid from the line" in {
      val function1: String => Option[String] = unitUnderTest.verify
      val result = function1(uuid)
      result shouldBe Some(uuid)
    }
  }

  "verify" should {
    "return None for an invalid uuid" in {
      val function1: String => Option[String] = unitUnderTest.verify
      val result = function1("not a valid uuid")
      result shouldBe None
    }
  }

}
