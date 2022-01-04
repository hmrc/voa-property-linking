/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.voapropertylinking.repositories.SequenceGeneratorMongoRepository
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder

import scala.concurrent.ExecutionContext

class SubmissionIdController @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      val sequenceGenerator: SequenceGeneratorMongoRepository
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  val charMapping: Map[Char, Char] = Map(
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
  ).withDefault(identity)

  def get(prefix: String): Action[AnyContent] = authenticated.async {
    sequenceGenerator.getNextSequenceId(prefix).map { id =>
      Ok(Json.toJson(formatId(prefix, id)))
    }
  }

  private[controllers] def formatId(prefix: String, id: Long): String =
    s"${prefix.toUpperCase}${BigInt(id).toString(26).map(charMapping).toUpperCase}"

}
