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

import com.google.inject.{ImplementedBy, Singleton}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

@ImplementedBy(classOf[EvidenceConnector])
trait EvidenceTransfer {
  def uploadFile(implicit hc: HeaderCarrier): Future[Unit]
}

@Singleton
class EvidenceConnector @Inject()(val ws: WSClient) extends EvidenceTransfer {
  override def uploadFile(implicit hc: HeaderCarrier): Future[Unit] = {
    //val res = ws.url(url)
    //  .post(Source(FilePart("fileName", "fileName", Option("contentType"), c ) :: List()))
    //res
    //TODO
    Future.successful(Nil)
  }
}
