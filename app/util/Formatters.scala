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

package util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.libs.json.{JsString, Writes}

object Formatters {

  def formatFilename(submissionId: String, fileName: String) = s"$submissionId-$fileName".replaceAll("[^A-Za-z0-9 .-]", " ");

  val voaLocalDateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

  implicit val writes = new Writes[LocalDateTime] {
    def writes(date: LocalDateTime) = JsString(voaLocalDateTimeFormat.format(date))
  }

}
