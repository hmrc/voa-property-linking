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

package connectors

import java.net.URLDecoder
import javax.inject.Inject

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import connectors.fileUpload.EnvelopeMetadata
import play.api.Logger
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

class EvidenceConnector @Inject()(val ws: WSClient) extends EvidenceTransfer with ServicesConfig with HandleErrors with AppName {
  lazy val url: String = baseUrl("external-business-rates-data-platform")
  lazy val uploadEndpoint: String = s"$url/customer-management-api/customer/evidence"
  lazy val voaApiKey: String = getString("voaApi.subscriptionKeyHeader")
  lazy val voaApiTrace: String = getString("voaApi.traceHeader")

  private def decode(fileName: String) = URLDecoder.decode(fileName, "UTF-8")

  override def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Uploading file: ${decode(fileName)}, subId: ${metadata.submissionId} to $uploadEndpoint")

    val res = ws.url(uploadEndpoint).withHeaders(
        ("Ocp-Apim-Subscription-Key", voaApiKey),
        ("Ocp-Apim-Trace", voaApiTrace),
        (USER_AGENT, appName)
      ).put(
        Source(
          FilePart("file", decode(fileName), Some("application/octet-stream"), content) ::
          DataPart("customerId", metadata.personId.toString) ::
          DataPart("filename", decode(fileName)) ::
          DataPart("submissionId", metadata.submissionId) ::
          Nil
        )
    ) map logResponse(fileName, metadata.submissionId)
    handleErrors(res, uploadEndpoint) map logError(fileName, metadata.submissionId)
  }

  def logResponse(fileName: String, subId: String): WSResponse => WSResponse = { r =>
    Logger.info(s"File upload completed: ${decode(fileName)}, subId: $subId to $uploadEndpoint, status: ${r.status}")
    r
  }

  def logError(fileName: String, subId: String): WSResponse => Unit = { r =>
    Logger.info(s"Response from API manager for file ${decode(fileName)}: $r")
  }
}
