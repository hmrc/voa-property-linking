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
import com.codahale.metrics.MetricRegistry
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import connectors.fileUpload.EnvelopeMetadata
import infrastructure.SimpleWSHttp
import metrics.MetricsLogger
import play.api.Logger
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.play.config.{AppName, ServicesConfig}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

class EvidenceConnector @Inject()(val ws: SimpleWSHttp, override val metrics: Metrics) extends EvidenceTransfer with ServicesConfig with HandleErrors with AppName with MetricsLogger {
  lazy val url: String = baseUrl("external-business-rates-data-platform")
  lazy val uploadEndpoint: String = s"$url/customer-management-api/customer/evidence"
  lazy val voaApiKey: (String, String) = "Ocp-Apim-Subscription-Key" -> getString("voaApi.subscriptionKeyHeader")
  lazy val voaApiTrace: (String, String) = "Ocp-Apim-Trace" -> getString("voaApi.traceHeader")
  lazy val userAgent: (String, String) = USER_AGENT -> appName
  lazy val headers = Seq(voaApiKey, voaApiTrace, userAgent)

  // Temporary fix for windows character issue in filenames - will drop entries to manual with them in their names
  private def decode(fileName: String) = URLDecoder.decode(fileName, "UTF-8").replaceAll("""[:<>"/\\|\?\*]""", "-")

  override def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Uploading file: ${decode(fileName)}, subId: ${metadata.submissionId} to $uploadEndpoint")

    handleErrors(ws.buildRequest(uploadEndpoint).withHeaders(headers:_*).put {
        Source(FilePart("file", decode(fileName), Some("application/octet-stream"), content) ::
          DataPart("customerId", metadata.personId.toString) ::
          DataPart("filename", decode(fileName)) ::
          DataPart("submissionId", metadata.submissionId) ::
          Nil)
    } andThen logResponse(fileName, metadata.submissionId), uploadEndpoint) andThen logMetrics("modernized.upload") map (_ => ())
  }

  def logResponse(fileName: String, subId: String): PartialFunction[Try[WSResponse], Unit] = {
    case Success(r) => Logger.info(s"File upload completed: ${decode(fileName)}, subId: $subId to $uploadEndpoint, status: ${r.status}")
    case Failure(e) => Logger.error(s"File upload failure: ${decode(fileName)}, subId: $subId to $uploadEndpoint, exception: ${e.getMessage}")
  }
}
