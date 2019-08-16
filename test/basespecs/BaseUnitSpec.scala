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

package basespecs

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.test.AllMocks
import uk.gov.hmrc.voapropertylinking.utils.Cats

import scala.concurrent.ExecutionContext

abstract class BaseUnitSpec extends UnitSpec
  with BeforeAndAfterEach
  with BeforeAndAfterAll
  with AllMocks
  with Matchers
  with Inspectors
  with Inside
  with EitherValues
  with LoneElement
  with ScalaFutures
  with OptionValues
  with Cats {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val requestWithPrincipal = RequestWithPrincipal(FakeRequest(), Principal("external-id", "group-id"))

}
