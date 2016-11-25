/*
 * Copyright 2016 HM Revenue & Customs
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

import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.google.inject.{ImplementedBy, Singleton}
import play.api.libs.ws.WSClient
import play.api.mvc.MultipartFormData.{DataPart, FilePart}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(submissionId: String, externalId: String, fileName: String, status: String, content: Option[Array[Byte]])(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class EvidenceConnector @Inject()(val ws: WSClient) extends EvidenceTransfer with ServicesConfig {
  override def uploadFile(submissionId: String, externalId: String, fileName: String,
                          status: String, content: Option[Array[Byte]])(implicit hc: HeaderCarrier): Future[Unit] = {
    val url =  baseUrl("external-business-rates-data-platform") + "/evidence"
    val res = ws.url(url)
      .withHeaders(("X-Requested-With", "VOA_CCA"))
      .put(Source(
        content.map(c => List(FilePart("file", fileName, None, Source.single(ByteString(c))))).getOrElse(Nil) ++ (
          DataPart("customerId", externalId) ::
          DataPart("filename", fileName) ::
          DataPart("submissionId", submissionId) ::
          List())
      ))
    res.map(_ => ())
  }
}
