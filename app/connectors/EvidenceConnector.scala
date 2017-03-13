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
import com.google.inject.{ImplementedBy, Singleton}
import config.ApplicationConfig
import connectors.fileUpload.EnvelopeMetadata
import play.api.Logger
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(fileName: String, content: Option[Array[Byte]], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class EvidenceConnector @Inject()(val ws: WSClient) extends EvidenceTransfer with ServicesConfig with HandleErrors {

  val url = baseUrl("external-business-rates-data-platform")
  override def uploadFile(fileName: String, content: Option[Array[Byte]], metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    val endpoint = "/customer-management-api/customer/evidence"

    Logger.info(s"Uploading file $fileName to /customer/evidence")

    val res = ws.url(url + endpoint)
      .withHeaders(
        ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
        ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
      )
      .put(Source(
        content.map(c => List(FilePart("file", fileName, None, Source.single(ByteString(c))))).getOrElse(Nil) ++ (
          DataPart("customerId", metadata.personId.toString) ::
          DataPart("filename", fileName) ::
          DataPart("submissionId", metadata.submissionId) ::
          List())
      ))
    handleErrors(res, endpoint) map { r => Logger.info(s"Response from API manager: $r") }
  }
}
