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

package uk.gov.hmrc.voapropertylinking.connectors.mdtp

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.{Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.{ExecutionContext, Future}

trait HandleErrors {

  def handleErrors(res: Future[WSResponse], request: String)(implicit ec: ExecutionContext): Future[WSResponse] = {
    res.flatMap { r => r.status match {
      case s if s >= 400 && s <= 499 => throw Upstream4xxResponse(s"$request failed with status $s. Response body: ${r.body}", s, s)
      case s if s >= 500 && s <= 599 => throw Upstream5xxResponse(s"$request failed with status $s. Response body: ${r.body}", s, s)
      case _ => res
    }}
  }
}
