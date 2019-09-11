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

package controllers

import java.util.UUID

import basespecs.BaseControllerSpec
import models.EnvelopeStatus.EnvelopeStatus
import org.mockito.ArgumentMatchers.{eq => matching, _}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.circuitbreaker.UnhealthyServiceException
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.{EnvelopeMetadata, FileUploadConnector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EnvelopeControllerSpec extends BaseControllerSpec {

  "EnvelopeController.create" should {
    "create a new envelope in FUaaS" in {
      val submissionId = UUID.randomUUID().toString

      val metadataJson = Json.obj("submissionId" -> submissionId, "personId" -> 1)

      val res = testController.create()(FakeRequest().withBody(metadataJson).withHeaders(HOST -> "localhost:9524"))
      status(res) shouldBe OK

      verify(mockFileUpload, once).createEnvelope(matching(EnvelopeMetadata(submissionId, 1)), matching(callbackUrl))(any[HeaderCarrier])
    }

    "record the envelope ID in mongo" in {
      val submissionId = UUID.randomUUID().toString
      val envelopeId = UUID.randomUUID().toString
      val metadataJson = Json.obj("submissionId" -> submissionId, "personId" -> 1)
      val metadata = EnvelopeMetadata(submissionId, 1)

      when(mockFileUpload.createEnvelope(matching(metadata), matching(callbackUrl))(any[HeaderCarrier])).thenReturn(Future.successful(Some(envelopeId)))

      val res = testController.create()(FakeRequest().withBody(metadataJson).withHeaders(HOST -> "localhost:9524"))
      status(res) shouldBe OK

      verify(mockRepo, once).create(matching(envelopeId), any())
    }

    "return the envelope ID as json" in {
      val submissionId = UUID.randomUUID().toString
      val metadataJson = Json.obj("submissionId" -> submissionId, "personId" -> 1)
      val metadata = EnvelopeMetadata(submissionId, 1)
      val envelopeId = UUID.randomUUID().toString

      when(mockFileUpload.createEnvelope(matching(metadata), matching(callbackUrl))(any[HeaderCarrier])) thenReturn Future.successful(Some(envelopeId))

      val res = testController.create()(FakeRequest().withBody(metadataJson).withHeaders(HOST -> "localhost:9524"))

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.obj("envelopeId" -> envelopeId)
    }

    "return a 503 Service Unavailable when file upload is not available" in {
      val metadataJson = Json.obj("submissionId" -> UUID.randomUUID().toString, "personId" -> 1)
      when(mockFileUpload.createEnvelope(any[EnvelopeMetadata], any[String])(any[HeaderCarrier])) thenReturn Future.failed {
        new UnhealthyServiceException("file upload isn't feeling well")
      }

      val res = testController.create()(FakeRequest().withBody(metadataJson))

      status(res) shouldBe SERVICE_UNAVAILABLE
      contentAsJson(res) shouldBe Json.obj("error" -> "file upload service not available")
    }

  }

  lazy val callbackUrl = routes.FileTransferController.handleCallback().absoluteURL()(FakeRequest().withHeaders(HOST -> "localhost:9524"))

  lazy val testController = new EnvelopeController(
    preAuthenticatedActionBuilders(),
    mockRepo,
    mockFileUpload,
    mockServicesConfig
  ) {
    override lazy val mdtpPlatformSsl = false
  }

  lazy val mockRepo = {
    val m = mock[EnvelopeIdRepo]
    when(m.create(anyString, any[EnvelopeStatus])) thenReturn Future.successful(())
    m
  }

  lazy val mockServicesConfig = {
    val m = mock[ServicesConfig]
    when(m.getBoolean(any[String])).thenReturn(false)
    m
  }
  lazy val mockFileUpload = {
    val m = mock[FileUploadConnector]
    when(m.createEnvelope(any[EnvelopeMetadata], matching(callbackUrl))(any[HeaderCarrier])) thenReturn Future.successful(Some(UUID.randomUUID().toString))
    m
  }

  lazy val once = times(1)

}

