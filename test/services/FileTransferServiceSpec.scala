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

import config.MongoDbProvider
import connectors.EvidenceConnector
import connectors.fileUpload.{EnvelopeMetadata, FileInfo, FileUploadConnector}
import helpers.WithSimpleWsHttpTestApplication
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import play.api.libs.ws.{StreamedResponse, WSResponseHeaders}
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.mongo.MongoConnector
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

object Result {
  sealed trait Result
  case object Ok extends Result
  case object Fail extends Result
}

class FileTransferServiceSpec extends UnitSpec with MockitoSugar {
  "The FileTransferService" should {
    val fileUploadConnector = mock[FileUploadConnector]
    val evidenceConnector = mock[EvidenceConnector]
    val repo = mock[EnvelopeIdRepo]

    val personId = 1L
    val fileInfo = FileInfo("id", "status", "name", "contentType", "created", "uniqueHref")
    val metaData = EnvelopeMetadata("submissionId", personId)

    val mockStreamedResponse = mock[StreamedResponse]
    val mockHeaders = mock[WSResponseHeaders]

    val fts = new FileTransferService(fileUploadConnector, evidenceConnector, repo) {
      override lazy val mongoConnector = mock[MongoConnector]
    }

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
  }
}
