/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.EvidenceConnector
import connectors.fileUpload.{EnvelopeInfo, FileInfo, FileUploadConnector}
import org.scalatest.mock.MockitoSugar
import org.scalatest.PrivateMethodTester._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.test.Helpers._
import play.api.mvc.Request
import play.api.test.FakeRequest
import repositories.EnvelopeIdRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FileTransferServiceTest extends ServicesSpec with MockitoSugar {

  implicit val request = FakeRequest().withSession(token)
  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier.fromHeadersAndSession(request.headers, Some(request.session))
  "justDoIt when an envelopeId only exist in Mongo" should "only remove the envId from mongo" in {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val envelopeRepo = mock[EnvelopeIdRepository]
    object TestFileTransferService extends FileTransferService(fileUploadConnector, evidenceConnector) {
      override val repo = envelopeRepo
    }

    val a = Future(Seq[String]("1", "2"))
    when(envelopeRepo.get()) thenReturn(a)
    val fixedHC = hc(request)
    when(fileUploadConnector.getEnvelopeDetails("1")(fixedHC)) thenReturn (Future(EnvelopeInfo("1", "NOT_EXISTING", "VOA_CCA", "", Nil)))
    when(fileUploadConnector.getEnvelopeDetails("2")(fixedHC)) thenReturn (Future(EnvelopeInfo("2", "NOT_EXISTING", "VOA_CCA", "", Nil)))

    await(TestFileTransferService.justDoIt()(fixedHC))
    verify(envelopeRepo, times(1)).remove("1")
    verify(envelopeRepo, times(1)).remove("2")
    verify(fileUploadConnector, times(0)).deleteEnvelope(anyString)(anyObject())
  }
  "justDoIt when an envelope is CLOSED but has no files associated with it" should "be removed from Mongo and FUAAS" in {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val envelopeRepo = mock[EnvelopeIdRepository]
    object TestFileTransferService extends FileTransferService(fileUploadConnector, evidenceConnector) {
      override val repo = envelopeRepo
    }
    val a = Future(Seq[String]("1", "2"))
    when(envelopeRepo.get()) thenReturn(a)
    val fixedHC = hc(request)
    when(fileUploadConnector.getEnvelopeDetails("1")(fixedHC)) thenReturn (Future(EnvelopeInfo("1", "CLOSED", "VOA_CCA", "", Nil)))
    when(fileUploadConnector.getEnvelopeDetails("2")(fixedHC)) thenReturn (Future(EnvelopeInfo("2", "CLOSED", "VOA_CCA", "", Nil)))
    when(fileUploadConnector.deleteEnvelope("1")(fixedHC)) thenReturn (Future.successful(()))
    when(fileUploadConnector.deleteEnvelope("2")(fixedHC)) thenReturn (Future.successful(()))

    await(TestFileTransferService.justDoIt()(fixedHC))

    verify(envelopeRepo, times(1)).remove("1")
    verify(envelopeRepo, times(1)).remove("2")
    verify(fileUploadConnector, times(1)).deleteEnvelope("1")(fixedHC)
    verify(fileUploadConnector, times(1)).deleteEnvelope("2")(fixedHC)
  }
  "justDoIt when an envelope is CLOSED and has files associated with it" should "download and push all files, and delete envId from FUUAS and mongo" in {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val envelopeRepo = mock[EnvelopeIdRepository]
    object TestFileTransferService extends FileTransferService(fileUploadConnector, evidenceConnector) {
      override val repo = envelopeRepo
    }
    val a = Future(Seq[String]("1", "2"))
    when(envelopeRepo.get()) thenReturn (a)
    val fixedHC = hc(request)
    Seq("1", "2").map(id => {
      when(fileUploadConnector.getEnvelopeDetails(id)(fixedHC)) thenReturn (
        Future(EnvelopeInfo(id, "CLOSED", "VOA_CCA", "", Seq(FileInfo("id", "status", "name", "contentType", "created", id))))
        )
      when(fileUploadConnector.downloadFile(id)(fixedHC)) thenReturn (Future(id.toCharArray.map(_.toByte)))
      when(evidenceConnector.uploadFile(fixedHC)) thenReturn (Future.successful(()))
      when(fileUploadConnector.deleteEnvelope(id)(fixedHC)) thenReturn (Future.successful(()))

    })

    await(TestFileTransferService.justDoIt()(fixedHC))

    Seq("1", "2").map(id => {
      verify(fileUploadConnector, times(1)).downloadFile(id)(fixedHC)
      verify(evidenceConnector, times(2)).uploadFile(fixedHC)
      verify(fileUploadConnector, times(1)).deleteEnvelope(id)(fixedHC)
      verify(envelopeRepo, times(1)).remove(id)
    })
  }
  "transferFile" should "only upload a file is the download was successful" in {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val envelopeRepo = mock[EnvelopeIdRepository]
    object TestFileTransferService extends FileTransferService(fileUploadConnector, evidenceConnector) {
      override val repo = envelopeRepo
    }

    val fixedHC = hc(request)
    when(fileUploadConnector.downloadFile("1")(fixedHC)) thenReturn (Future("1".toCharArray.map(_.toByte)))
    when(evidenceConnector.uploadFile(fixedHC)) thenReturn ( Future.successful(()))
    await(TestFileTransferService.transferFile("1")(fixedHC).recover{ case _ => ()})
    verify(evidenceConnector, times(1)).uploadFile(fixedHC)
  }

  it should "not upload a file if the download failed" in {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val envelopeRepo = mock[EnvelopeIdRepository]
    object TestFileTransferService extends FileTransferService(fileUploadConnector, evidenceConnector) {
      override val repo = envelopeRepo
    }
    val fixedHC = hc(request)
    when(fileUploadConnector.downloadFile("1")(fixedHC)) thenReturn (Future.failed(new Exception(s"File fails")))
    when(evidenceConnector.uploadFile(fixedHC)) thenReturn ( Future.successful(()))
    await(TestFileTransferService.transferFile("1")(fixedHC).recover{ case _ => ()})
    verify(evidenceConnector, times(0)).uploadFile(fixedHC)
  }

}
