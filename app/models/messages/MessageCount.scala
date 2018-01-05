/*
 * Copyright 2018 HM Revenue & Customs
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

package models.messages

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class MessageCount(unread: Int, total: Int)

object MessageCount {
  //frontend format
  val writes: Writes[MessageCount] = Json.writes[MessageCount]
  //"modernised" format
  val reads: Reads[MessageCount] = (
    (__ \ "messageCount").read[Int] and
      (__ \ "totalMessagesCount").read[Int]
    )(MessageCount.apply _)

  implicit val format: Format[MessageCount] = Format(reads, writes)
}
