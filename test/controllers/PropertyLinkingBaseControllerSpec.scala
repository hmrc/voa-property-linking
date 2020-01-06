/*
 * Copyright 2020 HM Revenue & Customs
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

import basespecs.BaseControllerSpec
import play.api.http.HttpEntity.Strict
import play.api.libs.json.{JsValue, Json, OFormat}
import play.api.mvc.Result
import play.api.test.FakeRequest

import scala.concurrent.Future

class PropertyLinkingBaseControllerSpec extends BaseControllerSpec {

  trait Setup {

    object Controller extends PropertyLinkingBaseController

    case class Foo(bar: String)

    implicit val format: OFormat[Foo] = Json.format

    val function: Foo => Future[Result] =
      _ => Future.successful(Ok)

    val foo: Foo = Foo(bar = "FooName")
    val fooJson: JsValue = Json.toJson(foo)
    val barJson: JsValue = Json.obj()
  }

  "withJsonBody" should {
    "evaluate function body" when {
      "passed in body is a valid JSON of expected type" in new Setup {
        implicit val request: FakeRequest[JsValue] = FakeRequest().withBody(fooJson)
        Controller.withJsonBody[Foo](function).futureValue shouldBe Ok
      }
    }
    "return BadRequest with error" when {
      "passed in body can't be parsed from JSON as expected type" in new Setup {
        implicit val request: FakeRequest[JsValue] = FakeRequest().withBody(barJson)
        inside(Controller.withJsonBody[Foo](function).futureValue) {
          case Result(header, Strict(data, _)) =>
            header.status shouldBe BAD_REQUEST
            data.utf8String shouldBe """List((/bar,List(ValidationError(List(error.path.missing),WrappedArray()))))"""
        }
      }
    }
  }

}

