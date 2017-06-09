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

import javax.inject.Inject

import akka.stream.scaladsl.Source
import akka.util.ByteString
import com.google.inject.ImplementedBy
import config.ApplicationConfig
import connectors.fileUpload.EnvelopeMetadata
import play.api.Logger
import play.api.libs.ws.{StreamedResponse, WSClient, WSResponse}
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

class EvidenceConnector @Inject()(val ws: WSClient) extends EvidenceTransfer with ServicesConfig with HandleErrors {

  val url = baseUrl("external-business-rates-data-platform")

  override def uploadFile(fileName: String, content: Source[ByteString, _], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    val endpoint = "/customer-management-api/customer/evidence"

    Logger.info(s"Uploading file: $fileName, subId: ${metadata.submissionId} to /customer/evidence")

    val res = ws.url(url + endpoint)
      .withHeaders(
        ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
        ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
      ).put(
        Source(
          FilePart("file", fileName, Some("application/octet-stream"), content) ::
          DataPart("customerId", metadata.personId.toString) ::
          DataPart("filename", fileName) ::
          DataPart("submissionId", metadata.submissionId) ::
          Nil
        )
    ) map logResponse(fileName, metadata.submissionId)
    handleErrors(res, endpoint) map logError(fileName, metadata.submissionId)
  }

  def logResponse(fileName: String, subId: String): WSResponse => WSResponse = { r =>
    Logger.info(s"File upload completed: $fileName, subId: $subId to /customer/evidence, status: ${r.status}")
    r
  }

  def logError(fileName: String, subId: String): WSResponse => Unit = { r =>
    Logger.info(s"Response from API manager for file $fileName: $r")
  }
}
