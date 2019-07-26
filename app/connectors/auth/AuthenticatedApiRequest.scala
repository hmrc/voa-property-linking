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

package connectors.auth

import auth.RequestWithPrincipal
import play.api.mvc.WrappedRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

case class AuthenticatedApiRequest[A](requestWithPrincipal: RequestWithPrincipal[A])
  extends WrappedRequest[A](requestWithPrincipal) {

  implicit val headerCarrier: HeaderCarrier =
    HeaderCarrierConverter.fromHeadersAndSession(requestWithPrincipal.headers)

  val principal = requestWithPrincipal.principal
  val externalId = principal.externalId
  val groupId = principal.groupId

}
