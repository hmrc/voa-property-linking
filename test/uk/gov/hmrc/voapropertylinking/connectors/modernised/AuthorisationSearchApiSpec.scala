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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models.searchApi._
import models.{PaginationParams, PropertyRepresentation, PropertyRepresentations}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class AuthorisationSearchApiSpec extends BaseUnitSpec {

  val http = mock[DefaultHttpClient]
  val connector = new AuthorisationSearchApi(http, mock[ServicesConfig]) {
    override lazy val baseUrl: String = "http://some-uri"
  }

  "A manage agents" should {
    "find owner agents" in {
      val organisationId = 123

      when(http.GET[Agents](any())(any(), any(), any())).thenReturn(Future.successful(
        Agents(Seq(Agent("Test name 1", 123), Agent("Test name 2", 123)))
      ))

      connector.manageAgents(organisationId)(hc).futureValue.agents.size shouldBe 2
    }
  }

  "AuthorisationManagementApi.forAgent" should {
    "return the invalid code which is not a no Agent Flag code" in {

      when(http.GET[AgentAuthResultBE](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(
          AgentAuthResultBE(
            1, 10, 30, 50, Seq(AgentAuthorisation(
              authorisationId = 987654,
              authorisedPartyId = 34567890,
              status = "APPROVED",
              representationSubmissionId = "xyz123",
              submissionId = "123xyz",
              uarn = 123456,
              address = "The White House",
              localAuthorityRef = "VOA1",
              client = Client(123456, "Fake News Inc"),
              representationStatus = "APPROVED",
              checkPermission = "START_AND_CONTINUE",
              challengePermission = "START_AND_CONTINUE"
            ))
          )
        ))

      val pageParams = PaginationParams(0, 10, false)
      val organisationId = 98765

      val validPropertyRepresentation = PropertyRepresentation(
        authorisationId = 987654,
        billingAuthorityReference = "VOA1",
        submissionId = "xyz123",
        organisationId = 123456,
        organisationName = "Fake News Inc",
        address = "The White House",
        checkPermission = "START_AND_CONTINUE",
        challengePermission = "START_AND_CONTINUE",
        createDatetime = today,
        status = "APPROVED"
      )

      val validPropertyRepresentations = PropertyRepresentations(
        totalPendingRequests = 30,
        propertyRepresentations = Seq(validPropertyRepresentation)
      )

      val result: PropertyRepresentations = connector.forAgent(status = "APPROVED", organisationId, pageParams)(hc).futureValue
      result shouldBe validPropertyRepresentations
    }
  }


}
