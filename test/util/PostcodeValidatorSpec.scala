/*
 * Copyright 2017 HM Revenue & Customs
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

package util

import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class PostcodeValidatorSpec extends FlatSpec with Matchers {

  behavior of "PostcodeValidator"

    it should "validate a correct with space" in {
      PostcodeValidator.validate("AA1 1AA") shouldBe (true)
    }

  it should "validate a correct postcode without space" in {
    PostcodeValidator.validate("BN21FG") shouldBe (true)
  }

  it should "not validate a non postcode text" in {
    PostcodeValidator.validate("123456") shouldBe (false)
  }

  it should "not change a valid postcode to contain a space" in {
    PostcodeValidator.validateAndFormat("AA1 1AA") shouldBe (Some("AA1 1AA"))
  }

  it should " format a valid postcode to contain a space if it doesn't have one" in {
    PostcodeValidator.validateAndFormat("AA11AA") shouldBe (Some("AA1 1AA"))
  }

  it should " format a valid postcode with long prefix to contain a space if it doesn't have one" in {
    PostcodeValidator.validateAndFormat("AA1A1AA") shouldBe (Some("AA1A 1AA"))
  }


  it should "return None for formatting a non postcode text" in {
    PostcodeValidator.validateAndFormat("123456") shouldBe (None)
  }

  it should "return validate all postcodes in the all postcodes list" in {
    val source = Source.fromFile(getClass.getResource("/all_postcodes.txt").getFile())
    for(line <- source.getLines()) {
      withClue(s"using $line validate") { PostcodeValidator.validate(line) shouldBe (true) }
    }
  }
}
