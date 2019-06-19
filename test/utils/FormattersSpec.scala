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

package utils

import org.scalatest.{FlatSpec, Matchers}

class FormattersSpec extends FlatSpec with Matchers {

  "formatFilename" should "prepend the submissionId and remove all non-alphanumeric characters apart from a space, hyphen or full stop" in {
    Formatters.formatFilename("PL12345", "Test/£file   name£with bad»characters.pdf") shouldBe "PL12345-Test  file   name with bad characters.pdf"
  }

}