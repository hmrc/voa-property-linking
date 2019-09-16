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

package services

import java.util.concurrent.atomic.AtomicInteger

import basespecs.BaseUnitSpec
import helpers.AnswerSugar
import models.EnvelopeStatus._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.concurrent.Eventually._
import play.api.libs.ws.{StreamedResponse, WSResponseHeaders}
import reactivemongo.bson.BSONDateTime
import repositories.EnvelopeId
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.voapropertylinking.connectors.mdtp._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class FileTransferServiceSpec extends BaseUnitSpec with AnswerSugar {

  val makeEnvelopeId: Int => EnvelopeId =
    n => EnvelopeId(n.toString, n.toString, None, Some(BSONDateTime(System.currentTimeMillis)))

  "The FileTransferService" should {
    val personId = 1L
    val fileInfo = FileInfo("id", "status", "name", "contentType", "created", "uniqueHref")
    val metaData = EnvelopeMetadata("submissionId", personId)

    val fts = new FileTransferService(mockFileUploadConnector, mockEvidenceConnector, mockEnvelopeIdRepo, mockMetrics)

    "Throw an exception if the file upload service returned status >= 400" in {
      when(mockFileUploadConnector.downloadFile("uniqueHref")).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockWSResponseHeaders)
      when(mockWSResponseHeaders.status).thenReturn(500)

      whenReady(fts.transferFile(fileInfo, metaData).failed)(_ shouldBe an[FUAASDownloadException])
    }

    "Not throw an exception if the file upload service returned status = 200" in {
      when(mockFileUploadConnector.downloadFile("uniqueHref")).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockWSResponseHeaders)
      when(mockWSResponseHeaders.status).thenReturn(200)
      when(mockEvidenceConnector.uploadFile(any(), any(), any())(any())).thenReturn(Future.successful(()))
      fts.transferFile(fileInfo, metaData).futureValue
    }

    "Fail fast on the first failure with uploads" in {
      val envelopes: List[EnvelopeId] = List.tabulate(10)(makeEnvelopeId)

      when(mockEnvelopeIdRepo.get()).thenReturn(Future.successful(envelopes))
      when(mockEnvelopeIdRepo.delete(any())).thenReturn(Future(()))
      when(mockFileUploadConnector.getEnvelopeDetails(any())(any())).thenAnswer { invocation: InvocationOnMock =>
        val envelopeId = invocation.getArgument[String](0)

        Future.successful(EnvelopeInfo(envelopeId,
          "Open",
          Seq(FileInfo(envelopeId, "Open", "name", "contentType", "created", s"uniqueHref-$envelopeId")),
          EnvelopeMetadata("", 1L)))
      }

      val counter = new AtomicInteger(0)

      when(mockFileUploadConnector.downloadFile(any())(any())).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockWSResponseHeaders)
      when(mockWSResponseHeaders.status).thenReturn(200)
      when(mockFileUploadConnector.deleteEnvelope(any())(any())).thenReturn(Future.successful(()))

      when(mockEvidenceConnector.uploadFile(any(), any(), any())(any())).thenAnswer { invocation: InvocationOnMock =>
        if (counter.getAndIncrement() > 4)
          Future.failed(new Exception("Failure"))
        else
          Future.successful(())
      }

      whenReady(fts.justDoIt().failed)(_ shouldBe an[RuntimeException])

      verify(mockEvidenceConnector, times(6)).uploadFile(any(), any(), any())(any())
      verify(mockFileUploadConnector, times(5)).deleteEnvelope(any())(any())
    }

    "Not fail, but skip errors originating in file upload" in {
      val envelopes: List[EnvelopeId] = List.tabulate(10)(makeEnvelopeId)

      when(mockEnvelopeIdRepo.get()).thenReturn(Future.successful(envelopes))
      when(mockEnvelopeIdRepo.delete(any())).thenReturn(Future(()))
      when(mockFileUploadConnector.getEnvelopeDetails(any())(any())).thenAnswer { invocation: InvocationOnMock =>
        val envelopeId = invocation.getArgument[String](0)

        Future.successful(EnvelopeInfo(envelopeId,
          "Open",
          Seq(FileInfo(envelopeId, "Open", "name", "contentType", "created", s"uniqueHref-$envelopeId")),
          EnvelopeMetadata("", 1L)))
      }

      val counter = new AtomicInteger(0)

      val mock404StreamedResponse = mock[StreamedResponse]
      val mock404Headers = mock[WSResponseHeaders]
      when(mock404StreamedResponse.headers).thenReturn(mock404Headers)
      when(mock404Headers.status).thenReturn(404)

      when(mockFileUploadConnector.downloadFile(any())(any())).thenAnswer { invocation: InvocationOnMock =>
        if (counter.getAndIncrement() == 4)
          Future.successful(mock404StreamedResponse)
        else
          Future.successful(mockStreamedResponse)
      }

      when(mockStreamedResponse.headers).thenReturn(mockWSResponseHeaders)
      when(mockWSResponseHeaders.status).thenReturn(200)
      when(mockFileUploadConnector.deleteEnvelope(any())(any())).thenReturn(Future.successful(()))
      when(mockEvidenceConnector.uploadFile(any(), any(), any())(any())).thenReturn(Future.successful(()))

      fts.justDoIt().futureValue

      verify(mockEvidenceConnector, times(9)).uploadFile(any(), any(), any())(any())
      verify(mockFileUploadConnector, times(9)).deleteEnvelope(any())(any())
    }

    "delete envelope IDs that don't exist in FUaaS" in {
      val envelopeId = "999"

      when(mockEnvelopeIdRepo.get()).thenReturn(Future.successful(Seq(EnvelopeId(envelopeId, envelopeId, Some(CLOSED), Some(BSONDateTime(System.currentTimeMillis))))))
      when(mockFileUploadConnector.getEnvelopeDetails(mEq(envelopeId))(any[HeaderCarrier])) thenReturn {
        Future.successful(EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 1)))
      }

      when(mockEnvelopeIdRepo.delete(anyString)).thenReturn(Future.successful(()))

      fts.justDoIt().futureValue

      verify(mockEnvelopeIdRepo).delete(envelopeId)
      verify(mockEnvelopeIdRepo, never).create(envelopeId, CLOSED)
    }

    "move envelopes to the back of the queue if there are any other errors when retrieving envelope data" in {
      val envelopeId = "9999"

      when(mockEnvelopeIdRepo.get()).thenReturn(Future.successful(Seq(EnvelopeId(envelopeId, envelopeId, Some(CLOSED), Some(BSONDateTime(System.currentTimeMillis))))))
      when(mockFileUploadConnector.getEnvelopeDetails(mEq(envelopeId))(any[HeaderCarrier])) thenReturn {
        Future.successful(EnvelopeInfo(envelopeId, "UNKNOWN_ERROR", Nil, EnvelopeMetadata("nosubmissionid", 1)))
      }

      when(mockEnvelopeIdRepo.delete(anyString)).thenReturn(Future.successful(()))
      when(mockEnvelopeIdRepo.create(envelopeId, CLOSED)).thenReturn(Future.successful(()))

      Try {
        fts.justDoIt().futureValue
      }

      verify(mockEnvelopeIdRepo).delete(envelopeId)
      verify(mockEnvelopeIdRepo).create(envelopeId, CLOSED)
    }

    "Log metrics for opened and closed envelopes is invoked correctly" in {
      val service = new FileTransferService(mockFileUploadConnector, mockEvidenceConnector, mockEnvelopeIdRepo, mockMetrics)

      when(mockMetrics.defaultRegistry).thenReturn(mockMetricRegistry)
      when(mockEnvelopeIdRepo.get()).thenReturn(Future(Seq.empty[EnvelopeId]))

      service.justDoIt()(HeaderCarrier()).futureValue

      eventually(verify(mockMetricRegistry).meter(ArgumentMatchers.eq("mongo.envelope.queue-size.open")))
      eventually(verify(mockMetricRegistry).meter(ArgumentMatchers.eq("mongo.envelope.queue-size.closed")))
    }

    "transferManually" should {
      "do something" in {
        val envelopeId = "ENV123"
        val submissionId = "SUB123"
        val personId = 1L
        when(mockEnvelopeIdRepo.getStatus(envelopeId))
          .thenReturn(Future.successful(Some(CLOSED)))
        when(mockFileUploadConnector.getEnvelopeDetails(mEq(envelopeId))(any()))
          .thenReturn(Future.successful(
            EnvelopeInfo(
              id = "ID1",
              status = "OPEN",
              files = Seq.empty[FileInfo],
              metadata = EnvelopeMetadata(submissionId, personId)
            )))

        fts.transferManually(envelopeId).futureValue
      }
    }

  }
}
