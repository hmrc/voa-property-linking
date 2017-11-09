/*
 * Copyright 2017 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connectors.EvidenceConnector
import connectors.fileUpload.{EnvelopeInfo, EnvelopeMetadata, FileInfo, FileUploadConnector}
import helpers.AnswerSugar
import models.Closed
import org.mockito.ArgumentMatchers
import org.scalatest.concurrent.Eventually._
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws.{StreamedResponse, WSResponseHeaders}
import reactivemongo.bson.BSONDateTime
import repositories.{EnvelopeId, EnvelopeIdRepo}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object Result {
  sealed trait Result
  case object Ok extends Result
  case object Fail extends Result
}

class FileTransferServiceSpec extends UnitSpec with MockitoSugar with AnswerSugar with BeforeAndAfterEach {

  override protected def beforeEach(): Unit = {
    reset(evidenceConnector, fileUploadConnector)
  }

  lazy val evidenceConnector = mock[EvidenceConnector]
  lazy val fileUploadConnector = mock[FileUploadConnector]

  "The FileTransferService" should {
    val repo = mock[EnvelopeIdRepo]

    val personId = 1L
    val fileInfo = FileInfo("id", "status", "name", "contentType", "created", "uniqueHref")
    val metaData = EnvelopeMetadata("submissionId", personId)

    val mockStreamedResponse = mock[StreamedResponse]
    val mockHeaders = mock[WSResponseHeaders]
    val metrics = mock[Metrics]

    val fts = new FileTransferService(fileUploadConnector, evidenceConnector, repo, metrics)

    implicit val fakeHc = HeaderCarrier()

    "Throw an exception if the file upload service returned status >= 400" in {
      when(fileUploadConnector.downloadFile("uniqueHref")).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockHeaders)
      when(mockHeaders.status).thenReturn(500)

      await(fts.transferFile(fileInfo, metaData) map { _ => Result.Fail } recover { case _ => Result.Ok } map {
        case Result.Ok => ()
        case Result.Fail => fail("Expected failed future with exception but got a Success")
      })
    }

    "Not throw an exception if the file upload service returned status = 200" in {
      when(fileUploadConnector.downloadFile("uniqueHref")).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockHeaders)
      when(mockHeaders.status).thenReturn(200)
      when(evidenceConnector.uploadFile(any(), any(), any())(any())).thenReturn(Future.successful(()))

      await(fts.transferFile(fileInfo, metaData) map { _ => Result.Ok } recover { case _ => Result.Fail } map {
        case Result.Ok => ()
        case Result.Fail => fail("Expected successful future")
      })
    }

    "Fail fast on the first failure with uploads" in {
      val envelopes: Seq[EnvelopeId] = Seq(
        EnvelopeId("1", "1", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("2", "2", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("3", "3", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("4", "4", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("5", "5", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("6", "6", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("7", "7", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("8", "8", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("9", "9", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("10", "10", None, Some(BSONDateTime(System.currentTimeMillis)))
      )

      when(repo.get()).thenReturn(Future.successful(envelopes))
      when(repo.delete(any())).thenReturn(Future(()))
      when(fileUploadConnector.getEnvelopeDetails(any())(any())).thenAnswer { invocation: InvocationOnMock =>
        val envelopeId = invocation.getArgument[String](0)

        Future.successful(EnvelopeInfo(envelopeId,
          "Open",
          Seq(FileInfo(envelopeId, "Open", "name", "contentType", "created", s"uniqueHref-$envelopeId")),
          EnvelopeMetadata("", 1L)))
      }

      val counter = new AtomicInteger(0)

      when(fileUploadConnector.downloadFile(any())(any())).thenReturn(Future.successful(mockStreamedResponse))
      when(mockStreamedResponse.headers).thenReturn(mockHeaders)
      when(mockHeaders.status).thenReturn(200)
      when(fileUploadConnector.deleteEnvelope(any())(any())).thenReturn(Future.successful(()))

      when(evidenceConnector.uploadFile(any(), any(), any())(any())).thenAnswer { invocation: InvocationOnMock =>
        if(counter.getAndIncrement() > 4)
          Future.failed(new Exception("Failure"))
        else
          Future.successful(())
      }

      await(fts.justDoIt() map { _ => Result.Fail } recover { case _ => Result.Ok } map {
        case Result.Ok =>
          verify(evidenceConnector, times(6)).uploadFile(any(), any(), any())(any())
          verify(fileUploadConnector, times(5)).deleteEnvelope(any())(any())
        case Result.Fail => fail("Expected failed future")
      })
    }

    "Not fail, but skip errors originating in file upload" in {
      val envelopes: Seq[EnvelopeId] = Seq(
        EnvelopeId("1", "1", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("2", "2", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("3", "3", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("4", "4", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("5", "5", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("6", "6", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("7", "7", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("8", "8", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("9", "9", None, Some(BSONDateTime(System.currentTimeMillis))),
        EnvelopeId("10", "10", None, Some(BSONDateTime(System.currentTimeMillis)))
      )

      when(repo.get()).thenReturn(Future.successful(envelopes))
      when(repo.delete(any())).thenReturn(Future(()))
      when(fileUploadConnector.getEnvelopeDetails(any())(any())).thenAnswer { invocation: InvocationOnMock =>
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

      when(fileUploadConnector.downloadFile(any())(any())).thenAnswer{ invocation: InvocationOnMock =>
        if(counter.getAndIncrement() == 4)
          Future.successful(mock404StreamedResponse)
        else
          Future.successful(mockStreamedResponse)
      }

      when(mockStreamedResponse.headers).thenReturn(mockHeaders)
      when(mockHeaders.status).thenReturn(200)
      when(fileUploadConnector.deleteEnvelope(any())(any())).thenReturn(Future.successful(()))
      when(evidenceConnector.uploadFile(any(), any(), any())(any())).thenReturn(Future.successful(()))

      await(fts.justDoIt() map { _ => Result.Ok } recover { case _ => Result.Fail } map {
        case Result.Ok =>
          verify(evidenceConnector, times(9)).uploadFile(any(), any(), any())(any())
          verify(fileUploadConnector, times(9)).deleteEnvelope(any())(any())
        case Result.Fail => fail("Expected future success")
      })
    }

    "delete envelope IDs that don't exist in FUaaS" in {
      val envelopeId = "999"

      when(repo.get()).thenReturn(Future.successful(Seq(EnvelopeId(envelopeId, envelopeId, Some(Closed), Some(BSONDateTime(System.currentTimeMillis))))))
      when(fileUploadConnector.getEnvelopeDetails(mEq(envelopeId))(any[HeaderCarrier])) thenReturn {
        Future.successful(EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 1)))
      }

      when(repo.delete(anyString)).thenReturn(Future.successful(()))

      await(fts.justDoIt())

      verify(repo, times(1)).delete(envelopeId)
      verify(repo, never).create(envelopeId, Closed)
    }

    "move envelopes to the back of the queue if there are any other errors when retrieving envelope data" in {
      val envelopeId = "9999"

      when(repo.get()).thenReturn(Future.successful(Seq(EnvelopeId(envelopeId, envelopeId, Some(Closed), Some(BSONDateTime(System.currentTimeMillis))))))
      when(fileUploadConnector.getEnvelopeDetails(mEq(envelopeId))(any[HeaderCarrier])) thenReturn {
        Future.successful(EnvelopeInfo(envelopeId, "UNKNOWN_ERROR", Nil, EnvelopeMetadata("nosubmissionid", 1)))
      }

      when(repo.delete(anyString)).thenReturn(Future.successful(()))
      when(repo.create(envelopeId, Closed)).thenReturn(Future.successful(()))

      Try { await(fts.justDoIt()) }

      verify(repo, times(1)).delete(envelopeId)
      verify(repo, times(1)).create(envelopeId, Closed)
    }

    "Log metrics for opened and closed envelopes is invoked correctly" in {
      val fileUploadConnector = mock[FileUploadConnector]
      val evidenceConnector = mock[EvidenceConnector]
      val envelopeIdRepo = mock[EnvelopeIdRepo]
      val metrics = mock[Metrics]
      val registry = mock[MetricRegistry]

      val service = new FileTransferService(fileUploadConnector, evidenceConnector, envelopeIdRepo, metrics)

      when(metrics.defaultRegistry).thenReturn(registry)
      when(envelopeIdRepo.get()).thenReturn(Future(Nil))

      await(service.justDoIt()(HeaderCarrier()))

      eventually(verify(registry, times(1)).meter(ArgumentMatchers.eq("mongo.envelope.queue-size.open")))
      eventually(verify(registry, times(1)).meter(ArgumentMatchers.eq("mongo.envelope.queue-size.closed")))
    }
  }
}
