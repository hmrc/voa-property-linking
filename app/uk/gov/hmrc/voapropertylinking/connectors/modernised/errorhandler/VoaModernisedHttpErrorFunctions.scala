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

package uk.gov.hmrc.voapropertylinking.connectors.modernised.errorhandler

import play.api.Logger
import uk.gov.hmrc.http.{HttpErrorFunctions, HttpResponse}
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes.REQUEST_URI_TOO_LONG


trait VoaModernisedHttpErrorFunctions extends HttpErrorFunctions {

  private val logger: Logger = play.api.Logger(this.getClass)

  //This Handle Response will pass everything through it possibly be a user error.
  override def handleResponse(verb: String, url: String)(response: HttpResponse): HttpResponse =
    response.status match {
      case REQUEST_URI_TOO_LONG.code =>
        throw VoaClientException("The request URI is too long.", 414)
      case status if is4xx(status) =>
        logger.debug(upstreamResponseMessage(verb, url, status, response.body))
        throw VoaClientException(response.body, status)
      case _ => super.handleResponse(verb, url)(response)
    }

}