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

package models.searchApi

import play.api.libs.json.Json

case class AgentAuthResultFE(
                              start: Int,
                              size: Int,
                              filterTotal: Int,
                              total: Int,
                              pendingRepresentations: Int,
                              authorisations: Seq[AgentAuthorisation]
                            )

object AgentAuthResultFE {
  implicit val agentAuthResult = Json.format[AgentAuthResultFE]

  def apply(results: AgentAuthResultBE, pendingRepresentationsCount: Int, approvedUnfilteredCount: Int): AgentAuthResultFE = {
    AgentAuthResultFE(
      start = results.start,
      size = results.size,
      filterTotal = results.filterTotal,
      total = approvedUnfilteredCount,
      pendingRepresentations = pendingRepresentationsCount,
      authorisations = results.authorisations
    )
  }
}
