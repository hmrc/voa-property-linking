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

import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class SequenceGeneratorRepositorySpec
  extends UnitSpec
    with BeforeAndAfterEach
    with MongoSpecSupport
{
  val repository = new SequenceGeneratorMongoRepository(mongo()) {

  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "repository" should {
    "have an empty index initially" in {
      val noneRepresentationOfLongValue = 600000000
      await(repository.getNextSequenceId("test")) shouldBe noneRepresentationOfLongValue
    }
  }

}

