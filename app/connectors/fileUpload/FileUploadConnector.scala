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

import com.google.inject.{ImplementedBy, Singleton}
import config.Wiring
import connectors.HandleErrors
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

object EnvelopeInfo {
  implicit lazy val envelopeInfo: Reads[EnvelopeInfo] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "status").read[String] and
      (JsPath \ "destination").read[String] and
      (JsPath \ "application").read[String] and
      (JsPath \ "files").readNullable[Seq[FileInfo]].map(x => x.getOrElse(Nil))
    ) (EnvelopeInfo.apply _)
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
  def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo]

  def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]]

  def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]]

  def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class FileUploadConnector @Inject()(ws: WSClient)(implicit ec: ExecutionContext) extends FileUpload with ServicesConfig with HandleErrors {
  lazy val http = Wiring().http
  lazy val url = baseUrl("file-upload-backend")

  override def getEnvelopeDetails(envelopeId: String)(implicit hc: HeaderCarrier): Future[EnvelopeInfo] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId")
      .recover { case _ => EnvelopeInfo(envelopeId, "NOT_EXISTING", "VOA_CCA", "", Nil) }
  }

  override def getFilesInEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    http.GET[EnvelopeInfo](s"$url/file-upload/envelopes/$envelopeId").map(_.files.map(_.href))
  }

  override def downloadFile(href: String)(implicit hc: HeaderCarrier): Future[Array[Byte]] = {
    Logger.info(s"Downloading file from $url$href")
    val res = ws.url(s"$url$href").get()

    handleErrors(res, s"$url$href") map { _.body.getBytes }
  }

  override def deleteEnvelope(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Deleting envelopedId: $envelopeId from FUAAS")
    http.DELETE[HttpResponse](s"$url/file-upload/envelopes/$envelopeId").map(_ => ())
  }
}
