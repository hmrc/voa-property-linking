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
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import models.PaginationParams
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, PropertyLinksWithClient}
import models.modernised.externalpropertylink.myorganisations.{AgentList, PropertyLinkWithAgents, PropertyLinksWithAgents}
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class ExternalPropertyLinkApiSpec extends BaseUnitSpec {

  trait Setup {

    val address = "mock address"
    val baref = "mock baref"
    val agent = "mock agent"
    val status = "mock status"
    val sortField = "mock sort field"
    val sortOrder = "mock sort order"

    val searchParams = GetMyOrganisationPropertyLinksParameters(
      address = Some(address),
      baref = Some(baref),
      agent = Some(agent),
      client = None,
      status = Some(status),
      Some(sortField),
      Some(sortOrder))

    val getMyClientsSearchParams = GetMyClientsPropertyLinkParameters(
      address = Some(address),
      baref = Some(baref),
      client = Some(agent),
      status = Some(status),
      Some(sortField),
      Some(sortOrder),
      representationStatus = None)

    val emptySearchParams = GetMyOrganisationPropertyLinksParameters()

    val voaApiUrl = "http://voa-modernised-api/external-property-link-management-api"
    val agentAuthorisationsUrl = s"$voaApiUrl/my-organisation/agents/{agentCode}/property-links"
    val ownerAuthorisationUrl = s"$voaApiUrl/my-organisation/property-links/{propertyLinkId}"
    val ownerAuthorisationsUrl = s"$voaApiUrl/my-organisation/property-links"
    val clientAuthorisationUrl = s"$voaApiUrl/my-organisation/clients/all/property-links/{propertyLinkId}"
    val clientAuthorisationsUrl = s"$voaApiUrl/my-organisation/clients/all/property-links"
    val createPropertyLinkUrl = s"$voaApiUrl/my-organisation/property-links"
    val myOrganisationsAgentsUrl = s"$voaApiUrl/my-organisation/agents"
    val revokeClientsPropertyLinkUrl =
      s"$voaApiUrl/my-organisation/clients/all/property-links/{submissionId}/appointment"
    val httpstring = "VoaAuthedBackendHttp"

    val connector = new ExternalPropertyLinkApi(
      http = mock[VoaHttpClient],
      myAgentPropertyLinksUrl = agentAuthorisationsUrl,
      myOrganisationsPropertyLinksUrl = ownerAuthorisationsUrl,
      myOrganisationsPropertyLinkUrl = ownerAuthorisationUrl,
      myClientsPropertyLinkUrl = clientAuthorisationUrl,
      myClientsPropertyLinksUrl = clientAuthorisationsUrl,
      createPropertyLinkUrl = createPropertyLinkUrl,
      myOrganisationsAgentsUrl = myOrganisationsAgentsUrl,
      revokeClientsPropertyLinkUrl = revokeClientsPropertyLinkUrl
    )

    val paginationParams = PaginationParams(1, 1, true)
    val queryParams: Seq[(String, String)] = Seq(
      ("start", paginationParams.startPoint.toString),
      ("size", paginationParams.pageSize.toString),
      ("requestTotalRowCount", "true"))
  }

  "get my organisations property links" should {

    "build the correct query params and call the modernised layer" in new Setup {

      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.http.GET[PropertyLinksWithAgents](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyOrganisationsPropertyLinks(emptySearchParams, params = Some(paginationParams))
        .futureValue shouldBe mockReturnedPropertyLinks

      verify(connector.http)
        .GET(mEq(ownerAuthorisationsUrl), mEq(queryParams))(any(), any(), any(), any())
    }

  }

  "get my agent property links" should {

    "build the correct query params and call the modernised layer" in new Setup {
      val agentCode = 1
      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.http.GET[PropertyLinksWithAgents](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyAgentPropertyLinks(agentCode, emptySearchParams, params = paginationParams)
        .futureValue shouldBe mockReturnedPropertyLinks

      verify(connector.http)
        .GET(mEq(agentAuthorisationsUrl.replace("{agentCode}", agentCode.toString)), mEq(queryParams))(
          any(),
          any(),
          any(),
          any())
    }

  }

  "get my organisations single property link with submissionId" should {

    "build the correct url and calls the modernised layer" in new Setup {

      val mockReturnedPropertyLink: PropertyLinkWithAgents = mock[PropertyLinkWithAgents]

      when(connector.http.GET[Option[PropertyLinkWithAgents]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(mockReturnedPropertyLink)))

      connector.getMyOrganisationsPropertyLink("PL1").futureValue shouldBe Some(mockReturnedPropertyLink)

      verify(connector.http)
        .GET(mEq(ownerAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
    }
  }

  "get clients property links" should {

    "build the correct query params and call the modernised layer" in new Setup {

      val mockReturnedPropertyLinks: PropertyLinksWithClient = mock[PropertyLinksWithClient]

      when(connector.http.GET[PropertyLinksWithClient](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
        .futureValue shouldBe mockReturnedPropertyLinks

      val clientQueryParams = queryParams :+ ("address" -> address) :+ ("baref" -> baref) :+ ("client" -> agent) :+ ("status" -> status) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder)
      verify(connector.http).GET(mEq(clientAuthorisationsUrl), mEq(clientQueryParams))(any(), any(), any(), any())
    }

  }

  "get clients single property link with submissionId" should {

    "build the correct url and calls the modernised layer" in new Setup {

      val mockReturnedPropertyLink: ClientPropertyLink = mock[ClientPropertyLink]

      when(connector.http.GET[ClientPropertyLink](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLink))

      connector.getClientsPropertyLink("PL1").futureValue shouldBe mockReturnedPropertyLink

      verify(connector.http)
        .GET(mEq(clientAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
    }
  }

  "create property link" should {

    "call modernised createPropertyLink endpoint" in new Setup {

      val mockHttpResponse: HttpResponse = mock[HttpResponse]
      val mockVoaCreatePropertyLink: CreatePropertyLink = mock[CreatePropertyLink]

      when(mockVoaCreatePropertyLink.PLsubmissionId).thenReturn("PL123")

      when(
        connector.http
          .POST[CreatePropertyLink, HttpResponse](any(), any(), any())(any(), any(), any(), any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector.createPropertyLink(mockVoaCreatePropertyLink).futureValue shouldBe mockHttpResponse

      verify(connector.http)
        .POST(mEq(createPropertyLinkUrl), mEq(mockVoaCreatePropertyLink), mEq(Seq()))(
          any(),
          any(),
          any(),
          any(),
          any())
    }

  }

  "get my organisations agents" should {

    "return the agents list for the given organisation" in new Setup {

      val mockReturnedAgentList: AgentList = mock[AgentList]

      when(connector.http.GET[AgentList](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedAgentList))

      connector
        .getMyOrganisationsAgents()
        .futureValue shouldBe mockReturnedAgentList

      verify(connector.http)
        .GET(mEq(myOrganisationsAgentsUrl), mEq(List("requestTotalRowCount" -> "true")))(any(), any(), any(), any())
    }

  }

  "revoke client property" should {

    "revoke a client property" in new Setup {

      val mockHttpResponse: HttpResponse = mock[HttpResponse]

      when(connector.http.DELETE[HttpResponse](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector
        .revokeClientProperty("some-submissionId")
        .futureValue shouldBe ()

      verify(connector.http)
        .DELETE(any())(any(), any(), any(), any())
    }

  }

}
