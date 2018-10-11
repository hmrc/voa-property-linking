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

package controllers

import javax.inject.Inject

import auth.Authenticated
import connectors.auth.{AuthConnector, DefaultAuthConnector}
import play.api.libs.json.Json
import repositories.SequenceGeneratorMongoRepository

class SubmissionIdController @Inject()(val authConnector: DefaultAuthConnector,
                                       val sequenceGenerator: SequenceGeneratorMongoRepository)
  extends PropertyLinkingBaseController with Authenticated {

  def get(prefix: String) = authenticated { implicit  request =>
    for {
      id <- sequenceGenerator.getNextSequenceId(prefix)
      strId = formatId(id)
    } yield {
      Ok(Json.toJson(prefix.toUpperCase + strId))
    }
  }

  private def formatId(id: Long) = {
    val charMapping= Map(
      // we have 26 alpha numeric character as input, i.e. 0-9, A-P
      // we convert is to 0-9A-X with the following chars omitted:a, c, e, f, i, k, o
      'a' -> 'q',
      'c' -> 'r',
      'e' -> 'v',
      'f' -> 'w',
      'i' -> 'x',
      'k' -> 'y',
      'o' -> 'z'
      //no mapping for s, t, u as they are not allowed.
    )
    BigInt(id).toString(26).map(x => charMapping.getOrElse(x, x)).toUpperCase
  }

}
