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


import models.dvr.DetailedValuationRequest
import org.scalatest.BeforeAndAfterEach
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global

class DVRRepositorySpec
  extends UnitSpec
    with BeforeAndAfterEach
    with MongoSpecSupport
{
  val app = appBuilder.build()

  val repository = new DVRRepository(app.injector.instanceOf[ReactiveMongoComponent], "test", app.injector.instanceOf[ServicesConfig]) {
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

  private val organisationId = 1234
  private val assessmentRef = 9876

  "repository" should {
    "have an empty index initially" in {
      await(repository.indexes shouldBe Seq.empty)
    }
  }

  "repository" should {
    "create a DVRRecord in the repository with the provided organisationId and assessmentRef" in {
      await(repository.create(DetailedValuationRequest(
        1L,
        1L,
        1L,
        "DVR-1",
        1L,
        None,
        ""
      ))) shouldBe ()
    }
  }

  "repository" should {
    "return true if a record with the organisationId and assessmentRef exists" in {
      await(repository.create(DetailedValuationRequest(
        1L,
        organisationId,
        1L,
        "DVR-1",
        assessmentRef,
        None,
        ""
      )))

      await(repository.exists(organisationId, assessmentRef)) shouldBe true
    }
  }

  "repository" should {
    "return false if a record with the organisationId and assessmentRef does not exist" in {
      await(repository.exists(organisationId, assessmentRef)) shouldBe false
    }
  }

  "repository" should {
    "clear the DVRRecord in the repository with the provided organisationId" in {
      await(repository.clear(organisationId)) shouldBe ()
    }
  }

}

