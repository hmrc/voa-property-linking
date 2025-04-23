/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.http

import play.api.libs.json.JsObject
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.voapropertylinking.auth.Principal
import uk.gov.hmrc.voapropertylinking.config.AppConfig

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VoaHttpClient @Inject() (
      httpClient: HttpClientV2,
      appConfig: AppConfig
) {

  def getWithGGHeaders[T](
        url: String
  )(implicit hc: HeaderCarrier, principal: Principal, httpReads: HttpReads[T], ec: ExecutionContext): Future[T] = {
    val urlEndpoint: URL = new URL(s"$url")
    val additionalHeaders: Seq[(String, String)] = buildHeadersWithGG(principal)
    val sanitizedHeaderCarrier: HeaderCarrier = removeDisallowedHeaders(hc, outboundHeaderNotAllowedList)
    val updatedHeaderCarrier: HeaderCarrier = sanitizedHeaderCarrier.withExtraHeaders(additionalHeaders: _*)

    httpClient.get(urlEndpoint).setHeader(updatedHeaderCarrier.extraHeaders: _*).withProxy.execute[T]
  }

  def get[T](url: String)(implicit hc: HeaderCarrier, httpReads: HttpReads[T], ec: ExecutionContext): Future[T] = {
    val urlEndpoint = new URL(s"$url")
    httpClient.get(urlEndpoint).execute[T]
  }

  def patch[T](url: String, body: JsObject)(implicit
        hc: HeaderCarrier,
        principal: Principal,
        httpReads: HttpReads[T],
        ec: ExecutionContext
  ): Future[T] = {
    val urlEndpoint: URL = new URL(s"$url")
    val additionalHeaders: Seq[(String, String)] = buildHeadersWithGG(principal)
    val sanitizedHeaderCarrier: HeaderCarrier = removeDisallowedHeaders(hc, outboundHeaderNotAllowedList)
    val updatedHeaderCarrier: HeaderCarrier = sanitizedHeaderCarrier.withExtraHeaders(additionalHeaders: _*)

    httpClient
      .patch(urlEndpoint)
      .setHeader(updatedHeaderCarrier.extraHeaders: _*)
      .withBody(body)
      .withProxy
      .execute[T]
  }

  def postWithGgHeaders[T](url: String, body: JsObject)(implicit
        hc: HeaderCarrier,
        principal: Principal,
        httpReads: HttpReads[T],
        ec: ExecutionContext
  ): Future[T] = {
    val urlEndpoint: URL = new URL(s"$url")
    val additionalHeaders: Seq[(String, String)] = buildHeadersWithGG(principal)
    val sanitizedHeaderCarrier: HeaderCarrier = removeDisallowedHeaders(hc, outboundHeaderNotAllowedList)
    val updatedHeaderCarrier: HeaderCarrier = sanitizedHeaderCarrier.withExtraHeaders(additionalHeaders: _*)

    httpClient
      .post(urlEndpoint)
      .setHeader(updatedHeaderCarrier.extraHeaders: _*)
      .withBody(body)
      .withProxy
      .execute[T]
  }

  def putWithGgHeaders[T](url: String, body: JsObject)(implicit
        hc: HeaderCarrier,
        principal: Principal,
        httpReads: HttpReads[T],
        ec: ExecutionContext
  ): Future[T] = {
    val urlEndpoint: URL = new URL(s"$url")
    val additionalHeaders: Seq[(String, String)] = buildHeadersWithGG(principal)
    val sanitizedHeaderCarrier: HeaderCarrier = removeDisallowedHeaders(hc, outboundHeaderNotAllowedList)
    val updatedHeaderCarrier: HeaderCarrier = sanitizedHeaderCarrier.withExtraHeaders(additionalHeaders: _*)

    httpClient
      .put(urlEndpoint)
      .setHeader(updatedHeaderCarrier.extraHeaders: _*)
      .withBody(body)
      .withProxy
      .execute[T]
  }

  def deleteWithGgHeaders[T](url: String)(implicit
                                          hc: HeaderCarrier,
                                          principal: Principal,
                                          httpReads: HttpReads[T],
                                          ec: ExecutionContext
  ): Future[T] = {
    val urlEndpoint = new URL(url)

    val ggHeaders = buildHeadersWithGG(principal)
    val baseHc = removeDisallowedHeaders(hc, outboundHeaderNotAllowedList)

    val commonHeaders = Seq(
      "Accept"        -> "application/json",
      "Content-Type"  -> "application/json",
      "Content-Length"-> "0"
    )

    val finalHc = baseHc.withExtraHeaders((ggHeaders ++ commonHeaders): _*)

    httpClient
      .delete(urlEndpoint)
      .setHeader(finalHc.extraHeaders: _*)
      .withProxy
      .execute[T]
  }


  def buildHeadersWithGG(principal: Principal): Seq[(String, String)] =
    Seq(
      "GG-EXTERNAL-ID"            -> principal.externalId,
      "GG-GROUP-ID"               -> principal.groupId,
      "Ocp-Apim-Subscription-Key" -> appConfig.apimSubscriptionKeyValue
    )

  val outboundHeaderNotAllowedList: Set[String] = Set(
    "Connection",
    "Content-Length",
    "Host",
    "Proxy-Authenticate",
    "Proxy-Authorization",
    "TE",
    "Transfer-Encoding",
    "Trailer",
    "Upgrade",
    "User-Agent"
  )

  def removeDisallowedHeaders(hc: HeaderCarrier, disallowedHeaders: Set[String]): HeaderCarrier = {
    val filteredExtraHeaders = hc.extraHeaders.filterNot { case (key, _) => disallowedHeaders.contains(key) }
    val filteredOtherHeaders = hc.otherHeaders.filterNot { case (key, _) => disallowedHeaders.contains(key) }
    hc.copy(extraHeaders = filteredExtraHeaders, otherHeaders = filteredOtherHeaders)
  }
}
