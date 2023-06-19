/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.bst

import basespecs.BaseUnitSpec
import org.mockito.ArgumentMatchers.{any, eq => matching}
import org.mockito.Mockito.{times, verify, when}
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation._

import scala.concurrent.Future

class ExternalOrganisationManagementApiSpec extends BaseUnitSpec {

  val testConnector = new ExternalOrganisationManagementApi(
    http = mockVoaHttpClient,
    agentAppointmentChangesUrl = "agentAppointmentChangesUrl",
    getAgentDetailsUrl = "getAgentDetailsUrl/{representativeCode}"
  )

  "OrganisationManagementApi.agentAppointmentChanges" should {
    "return appointmentChangeResponse for a valid appointment change request" in {

      when(
        mockVoaHttpClient.POST[AppointmentChangesRequest, AppointmentChangeResponse](any(), any(), any())(
          any(),
          any(),
          any(),
          any(),
          any()))
        .thenReturn(Future.successful(appointmentChangeResponse))

      testConnector
        .agentAppointmentChanges(
          AppointmentChangesRequest(
            agentRepresentativeCode = 12345L,
            action = AppointmentAction.APPOINT,
            scope = AppointmentScope.ALL_PROPERTIES,
            propertyLinks = None,
            listYears = None))
        .futureValue shouldBe appointmentChangeResponse

    }
  }

  "OrganisationManagementApi.getAgentDetails" should {
    "return AgentDetails with the provided agentCode" in {
      val agentCode = 123432L

      when(mockVoaHttpClient.GET[Option[AgentDetails]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(agentDetails)))

      testConnector.getAgentDetails(agentCode)(hc, requestWithPrincipal).futureValue shouldBe Some(agentDetails)

      verify(mockVoaHttpClient, times(1))
        .GET[Option[AgentDetails]](matching("getAgentDetailsUrl/123432"))(any(), any(), any(), any())
    }

    "return an None if no AgentDetails can be found for the provided agentCode" in {
      val agentCode = 123432L

      when(mockVoaHttpClient.GET[Option[AgentDetails]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getAgentDetails(agentCode)(hc, requestWithPrincipal).futureValue shouldBe None

      verify(mockVoaHttpClient, times(1))
        .GET[Option[AgentDetails]](matching("getAgentDetailsUrl/123432"))(any(), any(), any(), any())
    }
  }

}
