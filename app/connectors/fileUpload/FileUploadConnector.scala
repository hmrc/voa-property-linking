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

import com.google.inject.ImplementedBy
import connectors.HandleErrors
import infrastructure.SimpleWSHttp
import play.api.Logger
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.ws.{StreamedResponse, WSClient}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

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

  def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[StreamedResponse]

  def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit]
}

class FileUploadConnector @Inject()(ws: WSClient, http: SimpleWSHttp)(implicit ec: ExecutionContext)
  extends FileUpload with ServicesConfig with MicroserviceFilterSupport with AppName {
  lazy val url = baseUrl("file-upload-backend")

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

    ws.url(fullUrl)
      .withHeaders(USER_AGENT -> appName)
      .withHeaders(hc.headers: _*)
      .withMethod("GET")
      .stream() andThen handleResponse(fullUrl)
  }

  private def handleResponse(url: String): PartialFunction[Try[StreamedResponse],Unit] = {
    case Success(v) if v.headers.status < 400 => Logger.info(s"Transferred successfully from $url")
    case Success(v) if v.headers.status >= 400 => Logger.info(s"Transfer failed (${v.headers.status}) from $url")
    case Failure(ex) => Logger.error(s"Exception copying $url", ex)
  }

  override def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Deleting envelopeId: $envelopeId from FUAAS")
    http.DELETE[HttpResponse](s"$url/file-upload/envelopes/$envelopeId")
      .map { _ => () }
      .recover { case e => Logger.warn(s"Unable to delete envelope with ID: $envelopeId due to exception ${e.getMessage}") }
  }
}
