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

import connectors.auth.{AuthConnector, Authority, UserIds}
import connectors.{BusinessRatesAuthConnector, IndividualAccountConnector}
import models.{IndividualAccount, IndividualAccountId, IndividualAccountSubmission, IndividualDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class IndividualAccountControllerSpec extends ControllerSpec with MockitoSugar with WithFakeApplication {

  "create" should {
    "create a new individual user in modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccountSubmission = IndividualAccountSubmission("test-external-id", "test-trust-id", 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccountSubmission)

      when(mockIndividualAccountConnector.create(any())(any[HeaderCarrier])).thenReturn(Future.successful(IndividualAccountId(1)))

      val res = testController.create()(FakeRequest().withBody(individualJson))
      await(res)

      status(res) mustBe CREATED
      contentAsJson(res) mustBe Json.toJson(IndividualAccountId(1))
    }
  }

  "update" should {
    "update an individual user in modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccountSubmission = IndividualAccountSubmission("test-external-id", "test-trust-id", 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccountSubmission)
      val testJsonResponse =
        """
          |{
          |  "some": "json"
          |}
        """.stripMargin

      when(mockIndividualAccountConnector.update(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(Json.parse(testJsonResponse)))
      when(mockBrAuth.clearCache()(any[HeaderCarrier], any())).thenReturn(Future.successful(()))

      val res = testController.update(1)(FakeRequest().withBody(individualJson))
      await(res)

      status(res) mustBe OK
    }
  }

  "get" should {
    "return the json for an individual from modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccount = IndividualAccount("test-external-id", "test-trust-id", 1, 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccount)

      when(mockIndividualAccountConnector.get(any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testIndividualAccount)))

      val res = testController.get(1)(FakeRequest())
      await(res)

      status(res) mustBe OK
      contentAsJson(res) mustBe individualJson
    }

    "return NotFound for if the individual does not exist in modernised" in {
      when(mockIndividualAccountConnector.get(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val res = testController.get(1)(FakeRequest())
      await(res)

      status(res) mustBe NOT_FOUND
    }
  }

  "withExternalId" should {
    "return the json for an individual from modernised using the GG external ID" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccount = IndividualAccount("test-external-id", "test-trust-id", 1, 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccount)

      when(mockIndividualAccountConnector.findByGGID(any())(any[HeaderCarrier])).thenReturn(Future.successful(Some(testIndividualAccount)))

      val res = testController.withExternalId("test-external-id")(FakeRequest())
      await(res)

      status(res) mustBe OK
      contentAsJson(res) mustBe individualJson
    }

    "return NotFound for if the individual does not exist in modernised using the GG external ID" in {
      when(mockIndividualAccountConnector.findByGGID(any())(any[HeaderCarrier])).thenReturn(Future.successful(None))

      val res = testController.withExternalId("test-external-id")(FakeRequest())
      await(res)

      status(res) mustBe NOT_FOUND
    }
  }

  lazy val mockIndividualAccountConnector = mock[IndividualAccountConnector]

  lazy val mockBrAuth = mock[BusinessRatesAuthConnector]

  lazy val mockAuthConnector = {
    val m = mock[AuthConnector]
    when(m.getCurrentAuthority()(any())) thenReturn Future.successful(Some(Authority("userId", "userId", "userId", UserIds("userId", "userId"))))
    m
  }

  lazy val testController = new IndividualAccountController(mockAuthConnector, mockIndividualAccountConnector, mockBrAuth)

}

