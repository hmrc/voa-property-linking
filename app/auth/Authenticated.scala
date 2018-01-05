/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.auth.{AuthConnector, Authority}
import play.api.Logger
import play.api.mvc._
import play.api.mvc.Results._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait Authenticated {

  val auth: AuthConnector

  type UnauthenticatedUserHandler = () => Result

  implicit val uuh: UnauthenticatedUserHandler = () => Forbidden

  private implicit def hc(implicit request: Request[_]): HeaderCarrier =  HeaderCarrierConverter.fromHeadersAndSession(request.headers)

  def authenticated(block: Request[AnyContent] => Future[Result])
                      (implicit uuh: UnauthenticatedUserHandler): Action[AnyContent] =
    authenticated(BodyParsers.parse.default)(block)

  def authenticated[A](bodyParser: BodyParser[A])(block: Request[A] => Future[Result])
                   (implicit uuh: UnauthenticatedUserHandler): Action[A] =
    Action.async(bodyParser) { implicit request =>
      auth.getCurrentAuthority() flatMap {
        case None => Future.successful(uuh())
        case Some(authority) =>
          Logger.debug(s"Got authority = $authority")
          block(request)
      }
    }

}
