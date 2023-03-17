/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.jobHandler

import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{await, defaultAwaitTimeout, running}
import uk.gov.hmrc.voapropertylinking.repositories.{DVRRecord, DVRRepository}

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DvrRecordsJobHandlerSpec extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  "DvrRecordsJobHandler process job" should {
    "run updateStringCreatedAtTimestamp" in new Setup {
      when(mockDVRRepository.findIdsNoTimestamp)
        .thenReturn(Future.successful(Seq[ObjectId](new ObjectId())))
      when(mockDVRRepository.updateCreatedAtTimestampById(any())).thenReturn(Future.successful(1L))
      running(app) {
        await(handler.processJob())
        verify(mockDVRRepository).findIdsNoTimestamp
        await(mockDVRRepository.findIdsNoTimestamp)
        verify(mockDVRRepository).updateCreatedAtTimestampById(any())
      }
    }
  }

  trait Setup {

    val app: Application = GuiceApplicationBuilder()
      .overrides()
      .configure(
        "microservice.metrics.enabled" -> false,
        "metrics.enabled"              -> false,
        "auditing.enabled"             -> false,
        "housekeepingIntervalMinutes"  -> 1
      )
      .build()

    val mockDVRRepository: DVRRepository = mock[DVRRepository]
    val handler = new DvrRecordsJobHandler(mockDVRRepository)
    private val organisationId = 1234
    private val assessmentRef = 5678
    private val dvrSubmissionId = Some("dvrId")
    val dvrRecord = DVRRecord(organisationId, assessmentRef, None, dvrSubmissionId, Some(LocalDateTime.now()))
  }
}
