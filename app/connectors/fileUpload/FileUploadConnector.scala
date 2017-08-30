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

import javax.inject.Inject

import akka.stream.Materializer
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import infrastructure.SimpleWSHttp
import metrics.MetricsLogger
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.StreamedResponse
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class EnvelopeMetadata(submissionId: String, personId: Long)

object EnvelopeMetadata {
  implicit val format: Format[EnvelopeMetadata] = Json.format[EnvelopeMetadata]
}

case class EnvelopeConstraints(maxItems: Int, maxSize: String, contentTypes: Seq[String])

object EnvelopeConstraints {
  implicit lazy val format: Format[EnvelopeConstraints] = Json.format[EnvelopeConstraints]

  lazy val defaultConstraints = EnvelopeConstraints(1, "10MB", Seq("application/pdf", "image/jpeg"))
}

case class CreateEnvelopePayload(callbackUrl: String, metadata: EnvelopeMetadata, constraints: EnvelopeConstraints)

object CreateEnvelopePayload {
  implicit lazy val format: Format[CreateEnvelopePayload] = Json.format[CreateEnvelopePayload]
}

case class FileInfo(id: String,
                    status: String,
                    name: String,
                    contentType: String,
                    created: String,
                    href: String)

object FileInfo {
  implicit lazy val fileInfo: OFormat[FileInfo] = Json.format[FileInfo]
}

case class EnvelopeInfo(id: String,
                        status: String,
                        files: Seq[FileInfo],
                        metadata: EnvelopeMetadata)

object EnvelopeInfo {
  implicit lazy val envelopeInfo: Reads[EnvelopeInfo] = (
    (__ \ "id").read[String] and
      (__ \ "status").read[String] and
      (__ \ "files").readNullable[Seq[FileInfo]].map(x => x.getOrElse(Nil)) and
      (__ \ "metadata").read[EnvelopeMetadata]
    ) (EnvelopeInfo.apply _)
}

@ImplementedBy(classOf[FileUploadConnector])
trait FileUpload {
  def createEnvelope(metadata: EnvelopeMetadata, callbackUrl: String)(implicit hc: HeaderCarrier): Future[Option[String]]

  def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo]

  def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]]

  def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[StreamedResponse]

  def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit]
}

class FileUploadConnector @Inject()(http: SimpleWSHttp, override val metrics: Metrics)(implicit ec: ExecutionContext, mat: Materializer)
  extends FileUpload with ServicesConfig with AppName with MetricsLogger {

  lazy val url: String = baseUrl("file-upload-backend")

  override def createEnvelope(metadata: EnvelopeMetadata, callbackUrl: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val payload = CreateEnvelopePayload(callbackUrl, metadata, EnvelopeConstraints.defaultConstraints)
    http.POST[CreateEnvelopePayload, HttpResponse](s"$url/file-upload/envelopes", payload)
      .andThen(logMetrics("file-upload.envelope.create"))
      .map { _.header("location").flatMap { _.split("/").lastOption } }
      .recover { case _ => None }
  }

  override def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId").recover {
      case _: NotFoundException =>
        Logger.warn(s"Envelope $envelopeId not found")
        EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 0))
      case _ =>
        EnvelopeInfo(envelopeId, "UNKNOWN_ERROR", Nil, EnvelopeMetadata("nosubmissionid", 0))
    }
  }

  override def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId").map(_.files.map(_.href))
  }

  override def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[StreamedResponse] = {
    val fullUrl = s"$url$href"
    Logger.info(s"Downloading file from $fullUrl")
    http.buildRequest(fullUrl).withMethod("GET").stream() andThen logMetrics("file-upload.download") andThen handleResponse(fullUrl)
  }

  private def handleResponse(url: String): PartialFunction[Try[StreamedResponse], Unit] = {
    case Success(StreamedResponse(r, _)) if r.status < 400 => Logger.info(s"Transferred successfully from $url")
    case Success(v) => Logger.info(s"Transfer failed (${v.headers.status}) from $url")
    case Failure(ex) => Logger.error(s"Exception copying $url", ex)
  }

  override def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Deleting envelopeId: $envelopeId from FUAAS")

    http.DELETE[HttpResponse](s"$url/file-upload/envelopes/$envelopeId") andThen logMetrics("file-upload.envelope.delete") andThen {
      case Failure(e) => Logger.warn(s"Unable to delete envelope with ID: $envelopeId due to exception ${e.getMessage}")
    } map { _ => () } recover { case _ => () }
  }
}
