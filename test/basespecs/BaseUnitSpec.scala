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

package basespecs

import org.scalatest._
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.time.{Milliseconds, Second, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsNull
import play.api.mvc.AnyContentAsEmpty
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.test.AllMocks
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.utils.Cats
import utils.FakeObjects

import scala.concurrent.ExecutionContext

abstract class BaseUnitSpec
    extends AnyWordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with AllMocks with Inspectors
    with Inside with EitherValues with LoneElement with ScalaFutures with FakeObjects with MockitoSugar
    with PatienceConfiguration with DefaultAwaitTimeout with Cats {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val principal: Principal = Principal("external-id", "group-id")
  implicit val requestWithPrincipal: RequestWithPrincipal[AnyContentAsEmpty.type] =
    RequestWithPrincipal(FakeRequest(), principal)

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(Span(1, Second), Span(10, Milliseconds))

  val upperCased: BeMatcher[String] =
    BeMatcher(s => MatchResult(s == s.toUpperCase, "not all characters are upper-case", "found upper-case characters"))

  def emptyJsonHttpResponse(status: Int): HttpResponse =
    HttpResponse(status = status, json = JsNull, headers = Map[String, Seq[String]]())
}
