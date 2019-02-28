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

import modules.tasks.AddTimestamps
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.Environment
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import repositories.EnvelopeIdRepository
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class AddTimestampsSpec extends UnitSpec
  with BeforeAndAfterEach
  with MongoSpecSupport
  with MockitoSugar
  with WithFakeApplication{

  val repository = new EnvelopeIdRepository(fakeApplication.injector.instanceOf[ReactiveMongoComponent], s"${this.getClass.getSimpleName}") {
    override def indexes: Seq[Index] = Seq.empty
  }

  val mockEnv = mock[Environment]
  val unitUnderTest = new AddTimestamps(mockEnv, repository)
  val uuid = UUID.randomUUID().toString

  "run" should {
    "update the repo with the envelope" in {
      unitUnderTest.run(1) onSuccess {
        case r => r} shouldBe ()
    }
  }

}
