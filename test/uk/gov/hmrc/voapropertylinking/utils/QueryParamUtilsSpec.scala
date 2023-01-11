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

package uk.gov.hmrc.voapropertylinking.utils

import basespecs.BaseUnitSpec

class QueryParamUtilsSpec extends BaseUnitSpec {

  "converting a case class to a query string" should {
    "return a valid ordered query string" when {
      "the class contains primitive values" in {
        val caseClass = CaseClassWithPrimitives(1.toByte, 1.toShort, 'c', 1, 1.toLong, 1.toDouble, "string")

        val queryString = QueryParamUtils.toQueryString(caseClass)

        queryString shouldBe "byte=1&short=1&char=c&int=1&long=1&double=1.0&string=string"
      }
    }

    "return a valid ordered query string and skip optional values which are not present" when {
      "the class contain primitive values and optional values" in {
        val caseClass = CaseClassWithOptions("string", Some("string"), 1)

        val queryString = QueryParamUtils.toQueryString(caseClass)

        queryString shouldBe "string=string&optionString=string&int=1"
      }

      "the class contain primitive values and missing optional values" in {
        val caseClass = CaseClassWithOptions("string", None, 1)

        val queryString = QueryParamUtils.toQueryString(caseClass)

        queryString shouldBe "string=string&int=1"
      }
    }
  }
}

case class CaseClassWithOptions(string: String, optionString: Option[String], int: Int)

case class CaseClassWithPrimitives(
      byte: Byte,
      short: Short,
      char: Char,
      int: Int,
      long: Long,
      double: Double,
      string: String
)
