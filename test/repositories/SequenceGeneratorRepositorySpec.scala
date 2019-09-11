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
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}

import scala.concurrent.ExecutionContext.Implicits.global

class SequenceGeneratorRepositorySpec
  extends BaseUnitSpec
    with MongoSpecSupport {

  val repository = new SequenceGeneratorMongoRepository(new ReactiveMongoComponent {
    override def mongoConnector: MongoConnector = mongoConnectorForTest
  })

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.drop.futureValue
    repository.ensureIndexes.futureValue
  }

  "repository" should {
    "start sequences at 600M" in {
      val noneRepresentationOfLongValue = 600000000
      repository.getNextSequenceId("test").futureValue shouldBe noneRepresentationOfLongValue
    }

    "produce unique sequence numbers" in {
      val totalCount = 10
      val sequenceNumbers: List[Long] = (1 to totalCount).toList.traverse(_ => repository.getNextSequenceId("test")).futureValue
      sequenceNumbers.distinct should have size totalCount
    }

    "throw an exception if the current sequence value is too large" in {
      repository.insert(Sequence("foo", 999999999)).futureValue
      whenReady(repository.getNextSequenceId("foo").failed) { e =>
        e shouldBe a[RuntimeException]
        e.getMessage should startWith("Reached upper limit")
      }
    }
  }

}

