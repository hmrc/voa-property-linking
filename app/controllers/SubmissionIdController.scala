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

package controllers

import java.time.Instant
import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.Action

import scala.concurrent.Future
import scala.util.Random

class SubmissionIdController @Inject()()
  extends PropertyLinkingBaseController {

  def get(prefix: String) = Action.async { implicit  request =>
    val subId = prefix + genSubmissionId
    Future.successful(Ok(Json.toJson(subId)))
  }

  private def genSubmissionId: String = {
    val maxLength = 9
    val randomRange = 1000
    val charMapping= Map(
      // we have 26 alpha numeric character as input, i.e. 0-9, A-P
      'a' -> 'q',
      'c' -> 'r',
      'e' -> 'v',
      'f' -> 'w',
      'i' -> 'x',
      'k' -> 'y',
      'o' -> 'z'
      //no mapping for s, t, u as they are not allowed.
    )
    val millis = Instant.now.toEpochMilli * randomRange
    val rand = Random.nextInt(randomRange).toLong

    val strId = BigInt(millis + rand).toString(26).reverse
      .padTo(maxLength, '0')    //pad if less than 9 chars
      .substring(0, maxLength)  //only keep the first 9 chars (overflow protection)
      .reverse.toUpperCase
    strId.map(x => charMapping.getOrElse(x, x))
  }

}
