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

package controllers

import basespecs.BaseControllerSpec
import models.searchApi.{Agent, Agents}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.voapropertylinking.connectors.modernised.AuthorisationSearchApi

import scala.concurrent.Future

class AgentControllerSpec extends BaseControllerSpec {

  trait Setup {
    val mockAgentConnector = mock[AuthorisationSearchApi]

    val agentController = new AgentController(preAuthenticatedActionBuilders(), mockAgentConnector)
  }

  "given authorised access, manage agents" should {

    "return owner agents" in new Setup {
      when(mockConf.baseUrl(any())).thenReturn(baseUrl)
      reset(mockWS)

      val organisationId = 111

      val manageAgentUrl = s"$baseUrl/authorisation-search-api/owners/$organisationId/agents"

      val expected = Agents(agents = Seq(Agent("Name1", 1), Agent("Name2", 2)))
      when(mockAgentConnector.manageAgents(any())(any())).thenReturn(Future(expected))

      val res = agentController.manageAgents(organisationId)(FakeRequest())

      status(res) shouldBe OK
      val names = Json.parse(contentAsString(res)).as[Agents].agents

      names shouldBe Seq(Agent("Name1", 1), Agent("Name2", 2))
    }
  }
}


