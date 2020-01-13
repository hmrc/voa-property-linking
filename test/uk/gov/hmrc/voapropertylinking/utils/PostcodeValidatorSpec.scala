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

package uk.gov.hmrc.voapropertylinking.utils

import org.scalatest.{FlatSpec, Matchers}

class PostcodeValidatorSpec extends FlatSpec with Matchers {

  behavior of "PostcodeValidator"

  it should "validate a correct postcode with space" in {
    PostcodeValidator.validate("AA1 1AA") shouldBe true
  }

  it should "validate a correct postcode without a space" in {
    PostcodeValidator.validate("BN21FG") shouldBe true
  }

  it should "not validate a non postcode text" in {
    PostcodeValidator.validate("123456") shouldBe false
  }

  it should "not change a valid postcode to contain a space" in {
    PostcodeValidator.validateAndFormat("AA1 1AA") shouldBe Some("AA1 1AA")
  }

  it should "format a valid postcode to contain a space if it doesn't have one" in {
    PostcodeValidator.validateAndFormat("AA11AA") shouldBe Some("AA1 1AA")
  }

  it should "format a valid postcode with long prefix to contain a space if it doesn't have one" in {
    PostcodeValidator.validateAndFormat("AA1A1AA") shouldBe Some("AA1A 1AA")
  }

  it should "return None for formatting a non postcode text" in {
    PostcodeValidator.validateAndFormat("123456") shouldBe None
  }

  it should "validate all types of UK postcode" in {
    val postcodes = Seq(
      "AA11 1AA",
      "AA1A 1AA",
      "AA1 1AA",
      "A1A 1AA",
      "A1 1AA",
      "GIR 0AA"
    )

    postcodes foreach { pc =>
      withClue(s"validating postcode $pc") {
        PostcodeValidator.validate(pc) shouldBe true
      }
    }
  }
}
