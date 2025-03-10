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

object PostcodeValidator {
  val regexString =
    "(GIR 0AA)|((([A-Z-[QVX]][0-9][0-9]?)|(([A-Z-[QVX]][A-Z-[IJZ]][0-9][0-9]?)|(([A-Z-[QVX]][0-9][A-HJKPSTUW])|([A-Z-[QVX]][A-Z-[IJZ]][0-9][ABEHMNPRVWXY])))) *[0-9][A-Z-[CIKMOV]]{2})"
  private val postcodeRegex = regexString.r

  def validate(candidate: String): Boolean =
    candidate match {
      case postcodeRegex(_*) => true
      case _                 => false
    }

  def validateAndFormat(candidate: String): Option[String] =
    if (validate(candidate)) {
      val candidateNormalised = candidate.toUpperCase.replaceAll(" ", "")
      val (prefix, suffix) = candidateNormalised.splitAt(candidateNormalised.length - 3)
      Some(prefix + " " + suffix)
    } else
      None
}
