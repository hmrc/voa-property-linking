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

package controllers

import connectors.DVRCaseManagementConnector
import connectors.auth.{AuthConnector, Authority, UserIds}
import models.DetailedValuationRequest
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.DVRRecordRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DVRCaseManagementSpec extends ControllerSpec with MockitoSugar {

  val testDvr = DetailedValuationRequest(
    authorisationId = 123l,
    organisationId = 1l,
    personId = 2l,
    submissionId = "EMAIL123",
    assessmentRef = 3l,
    billingAuthorityReferenceNumber = "BAREF"
  )

  "requestDetailedValuation" should {
    "create a record of the DVR in mongo and POST the DVR to modernised" in {
      val dvrJson = Json.toJson(testDvr)
      val res = testController.requestDetailedValuation()(FakeRequest().withBody(dvrJson))

      await(res)
      verify(mockRepo, once).create(matching(1l), matching(3l))
      verify(mockDvrConnector, times(1)).requestDetailedValuation(matching(testDvr))(any[HeaderCarrier])
      status(res) mustBe OK
    }
  }

  "dvrExists" should {
    "return true if the DVR already exists in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((true))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      await(res)
      verify(mockRepo, times(1)).exists(matching(1l), matching(3l))
      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(true)
      reset(mockRepo)
    }

    "return false if the DVR does not exist in mongo" in {
      when(mockRepo.exists(anyLong(), anyLong())) thenReturn Future.successful((false))
      val res = testController.dvrExists(1l, 3l)(FakeRequest())

      await(res)
      verify(mockRepo, times(1)).exists(matching(1l), matching(3l))
      status(res) mustBe OK
      contentAsJson(res) mustBe Json.toJson(false)
      reset(mockRepo)
    }
  }

  lazy val testController = new DVRCaseManagement(mockAuthConnector, mockDvrConnector, mockRepo)

  lazy val mockRepo = {
    val m = mock[DVRRecordRepository]
    when(m.create(anyLong(), anyLong())) thenReturn Future.successful(())
    m
  }

  lazy val mockDvrConnector = {
    val m = mock[DVRCaseManagementConnector]
    when(m.requestDetailedValuation(any[DetailedValuationRequest])(any[HeaderCarrier])) thenReturn Future.successful()
    m
  }

  lazy val mockAuthConnector = {
    val m = mock[AuthConnector]
    when(m.getCurrentAuthority()(any())) thenReturn Future.successful(Some(Authority("userId", "userId", "userId", UserIds("userId", "userId"))))
    m
  }
}

