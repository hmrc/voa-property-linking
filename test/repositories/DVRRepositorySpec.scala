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
import models.voa.valuation.dvr.DetailedValuationRequest
import play.api.inject.guice.GuiceApplicationBuilder
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.Index
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global

class DVRRepositorySpec
  extends BaseUnitSpec
    with MongoSpecSupport {

  val app = new GuiceApplicationBuilder()
    .configure(
      "mongodb.uri" -> s"mongodb://127.0.0.1:27017/test-${this.getClass.getSimpleName}"
    ).build()

  val repository = new DVRRepository(app.injector.instanceOf[ReactiveMongoComponent], "test", app.injector.instanceOf[ServicesConfig])

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.drop.futureValue
    repository.ensureIndexes.futureValue
  }

  private val organisationId = 1234
  private val assessmentRef = 9876

  "repository" should {
    "have an index with TTL defined" in {
      inside(repository.indexes.loneElement) {
        case Index(_, Some(name), _, _, _, _, _, _, _) => name shouldBe "ttl"
      }
    }
  }

  "repository" should {
    "create a DVRRecord in the repository with the provided organisationId and assessmentRef" in {
      repository.create(DetailedValuationRequest(
        authorisationId = 1L,
        organisationId = 1L,
        personId = 1L,
        submissionId = "DVR-1",
        assessmentRef = 1L,
        agents = None,
        billingAuthorityReferenceNumber = ""
      )).futureValue shouldBe (())
    }
  }

  "repository" should {
    "return true if a record with the organisationId and assessmentRef exists" in {
      repository.create(DetailedValuationRequest(
        authorisationId = 1L,
        organisationId = organisationId,
        personId = 1L,
        submissionId = "DVR-1",
        assessmentRef = assessmentRef,
        agents = None,
        billingAuthorityReferenceNumber = ""
      )).futureValue

      repository.exists(organisationId, assessmentRef).futureValue shouldBe true
    }
  }

  "repository" should {
    "return false if a record with the organisationId and assessmentRef does not exist" in {
      repository.exists(organisationId, assessmentRef).futureValue shouldBe false
    }
  }

  "repository" should {
    "clear the DVRRecord in the repository with the provided organisationId" in {
      repository.clear(organisationId).futureValue shouldBe (())
    }
  }

}

