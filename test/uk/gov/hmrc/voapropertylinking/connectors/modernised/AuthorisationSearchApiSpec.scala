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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models.PaginationParams
import models.searchApi.{OwnerAuthResult => ModernisedAuthResult, _}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when

import scala.concurrent.Future

class AuthorisationSearchApiSpec extends BaseUnitSpec {

  val connector: AuthorisationSearchApi =
    new AuthorisationSearchApi(mockDefaultHttpClient, mockServicesConfig) {
      override lazy val baseUrl: String = "http://some-uri"
    }

  trait Setup {
    val orgId: Long = 1L
    val paginationParams: PaginationParams =
      PaginationParams(startPoint = 1, pageSize = 10, requestTotalRowCount = true)
    val agentId: Long = 2L
    val ownerAuthResult: ModernisedAuthResult = ModernisedAuthResult(1, 1, 1, 1, Seq())
  }

  "searching and sorting agents" should {
    "return what modernised returned" when {
      "called with minimal query parameters" in new Setup {
        when(mockDefaultHttpClient.GET[ModernisedAuthResult](any())(any(), any(), any()))
          .thenReturn(Future.successful(ownerAuthResult))

        val res: ModernisedAuthResult = connector.searchAndSort(orgId, paginationParams).futureValue

        res shouldBe ownerAuthResult
      }
    }
  }

  "appointableToAgent" should {
    "return what modernised returned" when {
      "called with minimal query parameters" in new Setup {
        when(mockDefaultHttpClient.GET[ModernisedAuthResult](any())(any(), any(), any()))
          .thenReturn(Future.successful(ownerAuthResult))

        val res: ModernisedAuthResult = connector
          .appointableToAgent(
            ownerId = orgId,
            agentId = agentId,
            params = paginationParams
          )
          .futureValue

        res shouldBe ownerAuthResult
      }
    }
  }

  "A manage agents" should {
    "find owner agents" in {
      val organisationId = 123

      when(mockDefaultHttpClient.GET[Agents](any())(any(), any(), any())).thenReturn(
        Future.successful(
          Agents(Seq(Agent("Test name 1", 123), Agent("Test name 2", 123)))
        ))

      connector.manageAgents(organisationId)(hc).futureValue.agents.size shouldBe 2
    }
  }

}
