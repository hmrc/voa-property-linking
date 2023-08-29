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

package uk.gov.hmrc.voapropertylinking.connectors.errorhandler

import uk.gov.hmrc.voapropertylinking.utils.Cats._
import uk.gov.hmrc.http.{HttpReads, HttpReadsInstances, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes._

trait VoaExceptionThrowingReads {

  implicit def voaReads[A](implicit hrds: HttpReads[A]): HttpReads[A] = {

    def mapToException(response: HttpResponse)(e: UpstreamErrorResponse): A =
      e.statusCode match {
        case REQUEST_URI_TOO_LONG.code =>
          throw VoaClientException("The request URI is too long.", 414).toUpstreamResponse
        case status if is4xx(status) =>
          throw VoaClientException(response.body, status).toUpstreamResponse
        case _ => throw e
      }

    HttpReads.ask.flatMap {
      // use case (method, url, response) if 'method' or 'url' are needed
      case (_, _, response) =>
        HttpReadsInstances.readEitherOf[A].map(_.leftMap(mapToException(response)).merge)
    }
  }
}
