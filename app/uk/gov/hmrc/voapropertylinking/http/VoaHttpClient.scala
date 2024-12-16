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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VoaHttpClient @Inject() (httpClient: DefaultHttpClient) {

  def buildHeaderCarrier(hc: HeaderCarrier, principal: Principal): HeaderCarrier =
    hc.copy()
      .withExtraHeaders(
        "GG-EXTERNAL-ID" -> principal.externalId,
        "GG-GROUP-ID"    -> principal.groupId
      )

  def GET[A](url: String)(implicit
        rds: HttpReads[A],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[A] =
    httpClient.GET[A](url)(implicitly, hc = buildHeaderCarrier(hc, requestWithPrincipal.principal), implicitly)

  def GET[A](url: String, queryParams: Seq[(String, String)])(implicit
        rds: HttpReads[A],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[A] =
    httpClient
      .GET[A](url, queryParams)(implicitly, hc = buildHeaderCarrier(hc, requestWithPrincipal.principal), implicitly)

  def DELETE[O](url: String)(implicit
        rds: HttpReads[O],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[O] =
    httpClient.DELETE[O](url)(implicitly, hc = buildHeaderCarrier(hc, requestWithPrincipal.principal), implicitly)

  def PUT[I, O](url: String, body: I)(implicit
        wts: Writes[I],
        rds: HttpReads[O],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[O] =
    httpClient.PUT[I, O](url, body)(
      implicitly,
      implicitly,
      hc = buildHeaderCarrier(hc, requestWithPrincipal.principal),
      implicitly
    )

  def POST[I, O](url: String, body: I, headers: Seq[(String, String)])(implicit
        wts: Writes[I],
        rds: HttpReads[O],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[O] =
    httpClient
      .POST[I, O](url, body, headers)(
        implicitly,
        implicitly,
        hc = buildHeaderCarrier(hc, requestWithPrincipal.principal),
        implicitly
      )

  def PATCH[I, O](url: String, body: I)(implicit
        wts: Writes[I],
        rds: HttpReads[O],
        hc: HeaderCarrier,
        ec: ExecutionContext,
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[O] =
    httpClient
      .PATCH[I, O](url, body)(
        implicitly,
        implicitly,
        hc = buildHeaderCarrier(hc, requestWithPrincipal.principal),
        implicitly
      )
}
