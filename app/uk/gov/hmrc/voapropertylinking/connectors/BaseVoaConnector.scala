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

package uk.gov.hmrc.voapropertylinking.connectors

import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import uk.gov.hmrc.uritemplate.syntax.UriTemplateSyntax
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaExceptionThrowingReads
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class BaseVoaConnector
    extends BaseConnector with VoaExceptionThrowingReads with HttpStatusCodes with UriTemplateSyntax {
  def toNone[T]: PartialFunction[T, None.type] = { case _ =>
    None
  }

  protected def getJsonWithGGHeaders[A: Reads: ClassTag](httpClient: VoaHttpClient, url: String)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[A] =
    httpClient.getWithGGHeaders[A](url)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readFromJson[A]),
      ec
    )

  protected def getOptionalJsonWithGGHeaders[A: Reads: ClassTag](httpClient: VoaHttpClient, url: String)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[Option[A]] =
    httpClient.getWithGGHeaders[Option[A]](url)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readOptionOfNotFound(HttpReads.Implicits.readFromJson[A])),
      ec
    )

  protected def getRawWithGGHeaders(httpClient: VoaHttpClient, url: String)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    httpClient.getWithGGHeaders[HttpResponse](url)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readRaw),
      ec
    )

  protected def postJsonWithGGHeaders[A: Reads: ClassTag](httpClient: VoaHttpClient, url: String, body: JsObject)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[A] =
    httpClient.postWithGgHeaders[A](url, body)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readFromJson[A]),
      ec
    )

  protected def postRawWithGGHeaders(httpClient: VoaHttpClient, url: String, body: JsObject)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    httpClient.postWithGgHeaders[HttpResponse](url, body)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readRaw),
      ec
    )

  protected def putJsonWithGGHeaders[A: Reads: ClassTag](httpClient: VoaHttpClient, url: String, body: JsObject)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[A] =
    httpClient.putWithGgHeaders[A](url, body)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readFromJson[A]),
      ec
    )

  protected def putRawWithGGHeaders(httpClient: VoaHttpClient, url: String, body: JsObject)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    httpClient.putWithGgHeaders[HttpResponse](url, body)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readRaw),
      ec
    )

  protected def deleteRawWithGGHeaders(httpClient: VoaHttpClient, url: String)(implicit
        request: RequestWithPrincipal[_],
        ec: ExecutionContext
  ): Future[HttpResponse] =
    httpClient.deleteWithGgHeaders[HttpResponse](url)(
      hc,
      principal,
      voaReads(HttpReads.Implicits.readRaw),
      ec
    )
}
