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

package controllers

import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}
import play.api.mvc.{Controller, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.Future

trait PropertyLinkingBaseController extends Controller {

  implicit def hc(implicit request: Request[_]): HeaderCarrier =  HeaderCarrierConverter.fromHeadersAndSession(request.headers)

  def withJsonBody[T](f: T => Future[Result])(implicit request: Request[JsValue], m: Manifest[T], reads: Reads[T]) = {
    request.body.validate[T] match {
      case JsSuccess(v, _) => f(v)
      case JsError(err) => Future.successful(BadRequest(err.toString))
    }
  }
}
