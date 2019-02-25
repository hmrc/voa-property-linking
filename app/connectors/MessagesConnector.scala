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

package connectors

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import javax.inject.{Inject, Named}
import models.messages.{MessageCount, MessageSearchParams, MessageSearchResults}
import play.api.libs.json.{JsNull, JsValue}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class MessagesConnector @Inject()(
                                   @Named("VoaBackendWsHttp") http: WSHttp,
                                   conf: ServicesConfig
                                 )(implicit ec: ExecutionContext) {

  lazy val baseUrl: String = conf.baseUrl("external-business-rates-data-platform")
  lazy val url = baseUrl + "/message-search-api"

  def getMessage(recipientOrgId: Long, messageId: String)(implicit hc: HeaderCarrier): Future[MessageSearchResults] = {
    http.GET[MessageSearchResults](s"$url/messages?recipientOrganisationID=$recipientOrgId&objectID=$messageId")
  }

  def getMessages(params: MessageSearchParams)(implicit hc: HeaderCarrier): Future[MessageSearchResults] = {
    http.GET[MessageSearchResults](s"$url/messages?${params.apiQueryString}")
  }

  def getMessageCount(orgId: Long)(implicit hc: HeaderCarrier): Future[MessageCount] = {
    http.GET[MessageCount](s"$url/count/$orgId")
  }

  def readMessage(messageId: String, readBy: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val now = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
    //no body required
    http.PATCH[JsValue, HttpResponse](s"$url/messages/$messageId?lastReadBy=$readBy&lastReadAt=$now", JsNull) map { _ => () }
  }
}
