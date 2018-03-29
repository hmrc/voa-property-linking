/*
 * Copyright 2018 HM Revenue & Customs
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

package connectors

import java.net.URLDecoder
import javax.inject.Inject

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import connectors.fileUpload.EnvelopeMetadata
import infrastructure.SimpleWSHttp
import metrics.MetricsLogger
import play.api.Logger
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

class EvidenceConnector @Inject()(val ws: SimpleWSHttp, override val metrics: Metrics) extends EvidenceTransfer with ServicesConfig with HandleErrors with AppName with MetricsLogger {
  lazy val url: String = baseUrl("external-business-rates-data-platform")
  lazy val uploadEndpoint = getString("endpoints.customerEvidence")
  lazy val filenameDecodingEnabled = getString("filenameDecoding.enabled").toBoolean
  lazy val uploadUrl: String = s"$url$uploadEndpoint"
  lazy val userAgent: (String, String) = USER_AGENT -> appName
  lazy val headers = Seq(userAgent)

  lazy val dangerousCharacterRegex = """[:<>"/\\|\?\*]"""

  private def replaceDangerousCharacters(fileName: String) = {
    if (filenameDecodingEnabled) {
      URLDecoder.decode(fileName, "UTF-8").replaceAll(dangerousCharacterRegex, "-")
    } else {
      fileName.replaceAll(dangerousCharacterRegex, "-")
    }
  }

  override def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Uploading file: ${replaceDangerousCharacters(fileName)}, subId: ${metadata.submissionId} to $uploadUrl")

    val fileData = Source(FilePart("file", replaceDangerousCharacters(fileName), Some("application/octet-stream"), content) ::
      DataPart("customerId", metadata.personId.toString) ::
      DataPart("filename", replaceDangerousCharacters(fileName)) ::
      DataPart("submissionId", metadata.submissionId) ::
      Nil)

    /*
    Temporary until we can completely move away from
    '/customer-management-api/customer/evidence' in production
    due to the fact '/case-documents-app-management-api/external/document'
    is a POST instead of a PUT.
     */
    uploadEndpoint match {
      case "/customer-management-api/customer/evidence" =>
        handleErrors(ws.buildRequest(uploadUrl).withHeaders(headers: _*).put {
          fileData
        } andThen logResponse(fileName, metadata.submissionId), s"PUT $uploadUrl") andThen logMetrics("modernized.upload") map (_ => ())
      case "/case-documents-app-management-api/external/document" =>
        handleErrors(ws.buildRequest(uploadUrl).withHeaders(headers: _*).post {
          fileData
        } andThen logResponse(fileName, metadata.submissionId), s"POST $uploadUrl") andThen logMetrics("modernized.upload") map (_ => ())
    }
  }

  def logResponse(fileName: String, subId: String): PartialFunction[Try[WSResponse], Unit] = {
    case Success(r) => Logger.info(s"File upload completed: ${replaceDangerousCharacters(fileName)}, subId: $subId to $uploadUrl, status: ${r.status}")
    case Failure(e) => Logger.error(s"File upload failure: ${replaceDangerousCharacters(fileName)}, subId: $subId to $uploadUrl, exception: ${e.getMessage}")
  }
}
