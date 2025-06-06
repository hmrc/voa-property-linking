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

package uk.gov.hmrc.voapropertylinking.binders.propertylinks

import basespecs.BaseUnitSpec
import cats.data.Validated.Valid

class GetMyClientsPropertyLinkParametersSpec extends BaseUnitSpec {

  import GetMyClientsPropertyLinkParameters._

  "validating GetMyClientsPropertyLinkParameters" should {
    "come out VALID" when {
      "no parameters are provided because all parameters are optional" in {
        val params = Map.empty[String, Seq[String]]
        val theExpectedValidThing = GetMyClientsPropertyLinkParameters()

        validate(params) shouldBe Valid(theExpectedValidThing)

        inside(binder.bind("", params)) { case Some(Right(ps)) =>
          ps shouldBe theExpectedValidThing
        }
      }
    }
  }

}
