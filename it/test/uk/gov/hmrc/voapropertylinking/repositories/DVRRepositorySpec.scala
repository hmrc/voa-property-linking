/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.repositories

import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.voapropertylinking.repositories.helpers.MongoSpec

import java.time.LocalDateTime

class DVRRepositorySpec extends MongoSpec {

  "create" should {
    "add one dvrRecord" in new Setup {
      await(repo.create(dvrRequest))
      repo.awaitCount shouldBe 1
    }
  }

  "find" should {
    "return a dvrRecord" when {
      "a dvrSubmissionId has not been defined" in new Setup {
        val noSubmissionId = dvrRecord.copy(dvrSubmissionId = None)
        repo.awaitInsert(noSubmissionId)

        await(repo.find(dvrRequest.organisationId, dvrRequest.assessmentRef)) shouldBe Some(noSubmissionId)
      }
      "a dvrRecord matches on orgId but not in agents seq of orgIds" in new Setup {
        val record = dvrRecord.copy(agents = Some(List(10L)))
        repo.awaitInsert(record)

        await(repo.find(dvrRequest.organisationId, dvrRequest.assessmentRef)) shouldBe Some(record)
      }
      "a dvrSubmission matches an agent orgId but not the IP orgId" in new Setup {
        repo.awaitInsert(dvrRecord)

        await(repo.find(90L, dvrRequest.assessmentRef)) shouldBe Some(dvrRecord)
      }
    }
    "return none" when {
      "no record matching assessmentRef exists" in new Setup {
        repo.awaitInsert(dvrRecord)
        repo.awaitCount shouldBe 1

        await(repo.find(dvrRequest.organisationId, 1L)) shouldBe None
      }
      "dvrRecord matches on assessmentRef but not on orgId returns none" in new Setup {
        repo.awaitInsert(dvrRecord)
        repo.awaitCount shouldBe 1

        await(repo.find(909L, dvrRequest.assessmentRef)) shouldBe None
      }
    }
  }

  "clear" should {
    "remove one dvrRecord" in new Setup {
      await(repo.create(dvrRequest))
      repo.awaitCount shouldBe 1
      await(repo.clear(dvrRequest.organisationId))
      repo.awaitCount shouldBe 0
    }
  }

  class Setup {
    val repo: DVRRepository = app.injector.instanceOf(classOf[DVRRepository])
    repo.removeAll
    await(repo.ensureIndexes)
    repo.awaitCount shouldBe 0

    val dvrRequest: DetailedValuationRequest = DetailedValuationRequest(
      authorisationId = 12L,
      organisationId = 34L,
      personId = 56L,
      submissionId = "DVR-123456",
      assessmentRef = 78L,
      agents = Some(List(90L)),
      billingAuthorityReferenceNumber = "01234"
    )

    val timestamp: LocalDateTime = LocalDateTime.of(2022, 12, 10, 1, 1, 1)
    val dvrRecord: DVRRecord =
      DVRRecord(
        dvrRequest.organisationId,
        dvrRequest.assessmentRef,
        dvrRequest.agents,
        Some(dvrRequest.submissionId),
        Some(timestamp))
  }
}
