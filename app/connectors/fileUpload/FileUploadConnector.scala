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

package connectors.fileUpload

import javax.inject.Inject

import com.google.inject.{ImplementedBy, Singleton}
import config.Wiring
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class EnvelopeInfo(
                        id: String,
                        status: String,
                        destination: String,
                        application: String,
                        files: Seq[FileInfo]
)

case class FileInfo(
                    id: String,
                    status: String,
                    name: String,
                    contentType: String,
                    created: String,
                    href: String
                   )

object FileInfo {
  implicit lazy val fileInfo = Json.format[FileInfo]
}

object EnvelopeInfo{
  //implicit lazy val envelopeInfo = Json.format[EnvelopeInfo]
  implicit lazy val envelopeInfo: Reads[EnvelopeInfo] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "destination").read[String] and
      (JsPath \ "application").read[String] and
      (JsPath \ "files").readNullable[Seq[FileInfo]].map(x => x.getOrElse(Nil))
    )(EnvelopeInfo.apply _)
}

case class NewEnvelope(envelopeId: String)
object NewEnvelope {
  implicit lazy val newEnvelope = Json.format[NewEnvelope]
}

case class RoutingRequest(envelopeId: String, application: String = "application/json", destination: String = "VOA_CCA")

object RoutingRequest {
  implicit lazy val routingRequest = Json.format[RoutingRequest]
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def getEnvelopeIds()(implicit hc: HeaderCarrier): Future[Seq[String]]
  def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo]
  def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]]
  def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]]
  def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier):Future[Unit]
}

@Singleton
class FileUploadConnector @Inject()(val ws: WSClient)(implicit ec: ExecutionContext) extends FileUpload with ServicesConfig with JsonHttpReads {
  lazy val http = Wiring().http

  override def getEnvelopeIds()(implicit hc: HeaderCarrier):Future[Seq[String]]  = {
    val url = s"${baseUrl("file-upload-backend")}/file-transfer/envelopes"
    val res = http.GET[HttpResponse](url)
    res.map { resp =>
      (resp.json \ "_embedded" \ "envelopes" \\ "id").map(_.toString)
    }
  }

  override def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo] = {
    val url = s"${baseUrl("file-upload-backend")}/file-upload/envelopes/${envelopeId}"
    http.GET[EnvelopeInfo](url)
      .recover { case _ => EnvelopeInfo(envelopeId, "NOT_EXISTING", "VOA_CCA", "", Nil)}
  }

  override def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    val url = s"${baseUrl("file-upload-backend")}/file-upload/envelopes/${envelopeId}"
    http.GET[EnvelopeInfo](url).map(_.files.map(_.href))
  }

  override def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    val url = s"${baseUrl("file-upload-backend")}/$href"
    http.GET[Array[Byte]](url)
  }

  override def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier):Future[Unit] = {
    val url = s"${baseUrl("file-upload-backend")}/file-upload/envelopes/$envelopeId"
    Logger.info(s"Deleting envelopedId: $envelopeId from FUAAS")
    http.DELETE[HttpResponse](url).map(_ => ())
  }
}

object DummyData {
    val envelopeContent =
      """
        |{
        |  "id": "8d227d24-4330-49b1-b405-145196a975b9",
        |  "status": "OPEN",
        |  "destination": "VOA_CCA",
        |  "application": "application/json",
        |  "files": [
        |    {
        |      "id": "index.jpeg",
        |      "status": "QUARANTINED",
        |      "name": "index.jpeg",
        |      "contentType": "image/jpeg",
        |      "created": "2016-11-09T15:50:59Z",
        |      "metadata": {},
        |      "href": "/file-upload/envelopes/8d227d24-4330-49b1-b405-145196a975b9/files/index.jpeg/content"
        |    },
        |    {
        |      "id": "index2.jpeg",
        |      "status": "QUARANTINED",
        |      "name": "index2.jpeg",
        |      "contentType": "image/jpeg",
        |      "created": "2016-11-09T15:50:59Z",
        |      "metadata": {},
        |      "href": "/file-upload/envelopes/8d227d24-4330-49b1-b405-145196a975b9/files/index2.jpeg/content"
        |    },
        |
        |  ]
        |}
      """.stripMargin
    val envelopesData = """{
      |  "_links": {
      |    "self": {
      |      "href": "http://full.url.com/file-transfer/envelopes?destination=DMS"
      |    }
      |  },
      |  "_embedded": {
      |    "envelopes": [
      |      {
      |        "id": "0b215e97-11d4-4006-91db-c067e74fc653",
      |        "destination": "DMS",
      |        "application": "application:digital.forms.service/v1.233",
      |        "_embedded": {
      |          "files": [
      |            {
      |              "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/1/content",
      |              "name": "original-file-name-on-disk.docx",
      |              "contentType": "application/vnd.oasis.opendocument.spreadsheet",
      |              "length": 1231222,
      |              "created": "2016-03-31T12:33:45Z",
      |              "_links": {
      |                "self": {
      |                  "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/1"
      |                }
      |              }
      |            },
      |            {
      |              "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/2/content",
      |              "name": "another-file-name-on-disk.docx",
      |              "contentType": "application/vnd.oasis.opendocument.spreadsheet",
      |              "length": 112221,
      |              "created": "2016-03-31T12:33:45Z",
      |              "_links": {
      |                "self": {
      |                  "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/2"
      |                }
      |              }
      |            }
      |          ]
      |        },
      |        "_links": {
      |          "self": {
      |            "href": "/file-transfer/envelopes/0b215e97-11d4-4006-91db-c067e74fc653"
      |          },
      |          "package": {
      |            "href": "/file-transfer/envelopes/0b215e97-11d4-4006-91db-c067e74fc653",
      |            "type": "application/zip"
      |          },
      |          "files": [
      |            {
      |              "href": "/files/2"
      |            }
      |          ]
      |        }
      |      }
      |    ]
      |  }
      |}"""
}

