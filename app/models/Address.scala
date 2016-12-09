/*
 * Copyright 2016 HM Revenue & Customs
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

package models

import play.api.libs.json.Json

case class Address(line1: String, line2: String, line3: String, postcode: String) {
  def id = 1 //TODO integrate with address api
}

object Address {
  implicit val formats = Json.format[Address]

  def fromLines(lines: Seq[String], postcode: String) = {
    def optionalLine(n: Int) = lines.lift(n).getOrElse("")

    require(lines.nonEmpty)
    Address(lines.head, optionalLine(1), optionalLine(2), postcode)
  }

  def fakeAddress = Address("Line 1", "Line 2", "Line 3", "AA11 1AA")
}
