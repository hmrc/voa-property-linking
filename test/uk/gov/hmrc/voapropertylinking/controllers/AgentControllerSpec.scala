/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import basespecs.BaseControllerSpec
import models.modernised.externalpropertylink.myorganisations.{AgentList, AgentSummary}
import models.searchApi.{Agent, Agents}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

class AgentControllerSpec extends BaseControllerSpec {

  trait Setup {
    val agentController =
      new AgentController(
        Helpers.stubControllerComponents(),
        preAuthenticatedActionBuilders(),
        mockExternalPropertyLinkApi)
  }

  "given authorised access, manage agents" should {

    "return owner agents" in new Setup {
      when(mockConf.baseUrl(any())).thenReturn(baseUrl)
      reset(mockWS)

      val organisationId = 111

      val yesterday = LocalDate.now().minusDays(1)

      val expected = AgentList(
        agents = List(
          AgentSummary(
            organisationId = 1L,
            name = "Name1",
            representativeCode = 1L,
            appointedDate = yesterday,
            propertyCount = 1),
          AgentSummary(
            organisationId = 2L,
            name = "Name2",
            representativeCode = 2L,
            appointedDate = yesterday,
            propertyCount = 1)
        ),
        resultCount = 2
      )
      when(mockExternalPropertyLinkApi.getMyOrganisationsAgents()(any())).thenReturn(Future(expected))

      val res = agentController.manageAgents(organisationId)(FakeRequest())

      status(res) shouldBe OK
      val names = Json.parse(contentAsString(res)).as[Agents].agents

      names shouldBe Seq(Agent("Name1", 1), Agent("Name2", 2))
    }
  }
}
