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

package auth

import connectors.auth.Principal
import play.api.mvc.{Request, WrappedRequest}

class RequestWithPrincipal[A](val wrappedRequest: Request[A], val principal: Principal)
  extends WrappedRequest[A](wrappedRequest)

object RequestWithPrincipal {
  def apply[A](request: Request[A], principal: Principal): RequestWithPrincipal[A] =
    new RequestWithPrincipal(request, principal)
}
