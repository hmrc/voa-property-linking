/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import basespecs.BaseControllerSpec
import models.{IndividualAccount, IndividualAccountId, IndividualAccountSubmission, IndividualDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised.CustomerManagementApi

import scala.concurrent.Future

class IndividualAccountControllerSpec extends BaseControllerSpec {

  "create" should {
    "create a new individual user in modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccountSubmission =
        IndividualAccountSubmission("test-external-id", "test-trust-id", 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccountSubmission)

      when(mockIndividualAccountConnector.createIndividualAccount(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(IndividualAccountId(1)))

      val res = testController.create()(FakeRequest().withBody(individualJson))

      status(res) shouldBe CREATED
      contentAsJson(res) shouldBe Json.toJson(IndividualAccountId(1))
    }
  }

  "update" should {
    "update an individual user in modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccountSubmission =
        IndividualAccountSubmission("test-external-id", "test-trust-id", 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccountSubmission)
      val testJsonResponse = """{ "some": "json" }"""

      when(mockIndividualAccountConnector.updateIndividualAccount(any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Json.parse(testJsonResponse)))
      when(mockBrAuth.clearCache()(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val res = testController.update(1)(FakeRequest().withBody(individualJson))

      status(res) shouldBe OK
    }
  }

  "get" should {
    "return the json for an individual from modernised" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccount = IndividualAccount("test-external-id", "test-trust-id", 1, 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccount)

      when(mockIndividualAccountConnector.getDetailedIndividual(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIndividualAccount)))

      val res = testController.get(1)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe individualJson
    }

    "return NotFound for if the individual does not exist in modernised" in {
      when(mockIndividualAccountConnector.getDetailedIndividual(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val res = testController.get(1)(FakeRequest())

      status(res) shouldBe NOT_FOUND
    }
  }

  "withExternalId" should {
    "return the json for an individual from modernised using the GG external ID" in {
      val testIndividualDetails = IndividualDetails("Test", "Name", "test@test.com", "01234556676", None, 1)
      val testIndividualAccount = IndividualAccount("test-external-id", "test-trust-id", 1, 1, testIndividualDetails)

      val individualJson = Json.toJson(testIndividualAccount)

      when(mockIndividualAccountConnector.findDetailedIndividualAccountByGGID(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(testIndividualAccount)))

      val res = testController.withExternalId("test-external-id")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe individualJson
    }

    "return NotFound for if the individual does not exist in modernised using the GG external ID" in {
      when(mockIndividualAccountConnector.findDetailedIndividualAccountByGGID(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val res = testController.withExternalId("test-external-id")(FakeRequest())

      status(res) shouldBe NOT_FOUND
    }
  }

  lazy val mockIndividualAccountConnector: CustomerManagementApi = mock[CustomerManagementApi]

  lazy val mockBrAuth: BusinessRatesAuthConnector = mock[BusinessRatesAuthConnector]

  lazy val testController = new IndividualAccountController(
    controllerComponents = Helpers.stubControllerComponents(),
    authenticated = preAuthenticatedActionBuilders(),
    customerManagementApi = mockIndividualAccountConnector,
    auditingService = mock[AuditingService],
    brAuth = mockBrAuth
  )

}
