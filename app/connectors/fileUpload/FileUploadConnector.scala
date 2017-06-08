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

package connectors.fileUpload

import java.io.ByteArrayOutputStream
import javax.inject.Inject

import akka.stream.scaladsl._
import akka.util.ByteString
import com.google.inject.ImplementedBy
import connectors.HandleErrors
import infrastructure.SimpleWSHttp
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class EnvelopeMetadata(submissionId: String, personId: Long)

object EnvelopeMetadata {
  implicit val format: Format[EnvelopeMetadata] = Json.format[EnvelopeMetadata]
}

case class EnvelopeInfo(
                         id: String,
                         status: String,
                         files: Seq[FileInfo],
                         metadata: EnvelopeMetadata
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

object EnvelopeInfo {
  implicit lazy val envelopeInfo: Reads[EnvelopeInfo] = (
    (__ \ "id").read[String] and
      (__ \ "status").read[String] and
      (__ \ "files").readNullable[Seq[FileInfo]].map(x => x.getOrElse(Nil)) and
      (__ \ "metadata").read[EnvelopeMetadata]
    ) (EnvelopeInfo.apply _)
}

case class RoutingRequest(envelopeId: String, application: String = "application/json", destination: String = "VOA_CCA")

object RoutingRequest {
  implicit lazy val routingRequest = Json.format[RoutingRequest]
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo]

  def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]]

  def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]]

  def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit]
}

class FileUploadConnector @Inject()(ws: WSClient, http: SimpleWSHttp)(implicit ec: ExecutionContext) extends FileUpload with ServicesConfig with HandleErrors with MicroserviceFilterSupport {
  lazy val url = baseUrl("file-upload-backend")

  override def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId")
      .recover { case _ => EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 0)) }
  }

  override def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId").map(_.files.map(_.href))
  }

  override def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    Logger.info(s"Downloading file from $url$href")

    val res = ws.url(s"$url$href").withMethod("GET").stream() flatMap { r =>
      val outputStream = new ByteArrayOutputStream()

      val sink = Sink.foreach[ByteString] { bytes =>
        outputStream.write(bytes.toArray)
      }

      r.body.runWith(sink).andThen {
        case result =>
          result.get
      } map { _ => outputStream.toByteArray }
    }
    
    res.map { r => Logger.info(s"Downloaded ${r.length / 1024}kB from $url$href") }
    res
  }

  override def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Deleting envelopeId: $envelopeId from FUAAS")
    http.DELETE[HttpResponse](s"$url/file-upload/envelopes/$envelopeId").map(_ => ())
  }

}
