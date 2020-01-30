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

package uk.gov.hmrc.voapropertylinking.binders.validation

import basespecs.BaseUnitSpec
import cats.Show
import org.scalatest.prop.Tables.Table
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor2}

class ValidationErrorSpec extends BaseUnitSpec {

  "validation errors" should {
    "show correct text" in {
      val scenarios: TableFor2[ValidationError, String] = Table(
        ("validation error", "expected string representation"),
        MissingError("key")                           -> """Missing value for parameter "key"""",
        InvalidTypeError("key", classOf[Long])("Foo") -> """Invalid type for parameter "key". "Foo" is not a valid Integer""",
        NotAnEnumError("key", Seq("A", "B"))("C")     -> """Invalid value "C" for parameter "key". Allowed values: [A, B]""",
        OverLimitError("key", 5)(10)                  -> """Value "10" for parameter "key" is over the acceptable limit: 5""",
        UnderLimitError("key", 5)(1)                  -> """Value "1" for parameter "key" is under the acceptable limit: 5""",
        OverMaxLengthError("key", 5)("foobar")        -> """Value for parameter "key" is longer than the the acceptable maximum: 5""",
        UnderMinLengthError("key", 5)("foo")          -> """Value for parameter "key" is shorter than the acceptable minimum: 5""",
        AllMissingError("key", "key2")                -> """At least one of these parameters must be provided: [key, key2]""",
        InvalidFormat("key")                          -> """Invalid format for parameter "key"""",
        BlankQueryParameterError("key")               -> """Missing value for parameter "key""""
      )

      TableDrivenPropertyChecks.forAll(scenarios) {
        case (e, expectedString) => Show[ValidationError].show(e) shouldBe expectedString
      }
    }
  }

}
