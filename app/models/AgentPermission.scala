/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.Format
import uk.gov.hmrc.voapropertylinking.utils.JsonUtils

object AgentPermission extends Enumeration {
  type AgentPermission = Value
  val StartAndContinue: AgentPermission = Value("START_AND_CONTINUE")
  val ContinueOnly: AgentPermission = Value("CONTINUE_ONLY")
  val NotPermitted: AgentPermission = Value("NOT_PERMITTED")

  implicit val format: Format[AgentPermission] = JsonUtils.enumFormat(AgentPermission)

  def fromName(name: String): Option[AgentPermission] =
    List(StartAndContinue, ContinueOnly, NotPermitted).collectFirst {
      case p if p.toString == name => p
    }
}
