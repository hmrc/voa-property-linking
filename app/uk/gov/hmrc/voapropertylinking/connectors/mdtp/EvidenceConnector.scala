/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.mdtp

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import com.kenshoo.play.metrics.Metrics
import infrastructure.SimpleWSHttp
import javax.inject.Inject
import metrics.MetricsLogger
import play.api.Logger
import play.api.http.HeaderNames.USER_AGENT
import play.api.libs.ws.WSResponse
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import uk.gov.hmrc.voapropertylinking.utils.FileNameSanitisationUtils.formatFileName

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

class EvidenceConnector @Inject()(
                                   val ws: SimpleWSHttp,
                                   override val metrics: Metrics,
                                   config: ServicesConfig
                                 ) extends EvidenceTransfer with HandleErrors with MetricsLogger {
  lazy val url: String = config.baseUrl("external-business-rates-data-platform")
  lazy val uploadEndpoint = config.getString("endpoints.customerEvidence")
  lazy val uploadUrl: String = s"$url$uploadEndpoint"
  lazy val userAgent: (String, String) = USER_AGENT -> config.getString("appName")
  lazy val headers = Seq(userAgent)

  override def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    Logger.info(s"Uploading file: ${formatFileName(metadata.submissionId, fileName)}, subId: ${metadata.submissionId} to $uploadUrl")

    val fileData = Source(FilePart("file", formatFileName(metadata.submissionId, fileName), Some("application/octet-stream"), content) ::
      DataPart("customerId", metadata.personId.toString) ::
      DataPart("filename", formatFileName(metadata.submissionId, fileName)) ::
      DataPart("submissionId", metadata.submissionId) ::
      Nil)

    handleErrors(ws.buildRequest(uploadUrl).withHeaders(headers: _*).post {
      fileData
    }, s"POST $uploadUrl") andThen logResponse(fileName, metadata.submissionId) andThen logMetrics("modernized.upload") map (_ => ())
  }

  def logResponse(fileName: String, subId: String): PartialFunction[Try[WSResponse], Unit] = {
    case Success(r) => Logger.info(s"File upload completed: ${formatFileName(subId, fileName)}, subId: $subId to $uploadUrl, status: ${r.status}")
    case Failure(e) => Logger.error(s"File upload failure: ${formatFileName(subId, fileName)}, subId: $subId to $uploadUrl, exception: ${e.getMessage}")
  }
}
