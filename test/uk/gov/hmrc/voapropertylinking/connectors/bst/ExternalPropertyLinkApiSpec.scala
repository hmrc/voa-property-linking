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
import models.PaginationParams
import models.modernised.{Capacity, Evidence, EvidenceType, ProvidedEvidence}
import models.modernised.externalpropertylink.myclients.{ClientPropertyLink, ClientsResponse, PropertyLinksWithClient}
import models.modernised.externalpropertylink.myorganisations.{AgentList, PropertyLinkWithAgents, PropertyLinksWithAgents}
import models.modernised.externalpropertylink.requests.{CreatePropertyLink, CreatePropertyLinkOnClientBehalf}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito.{when, _}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class ExternalPropertyLinkApiSpec extends BaseUnitSpec {

  trait Setup {

    val address = "mock address"
    val uarn = 123
    val baref = "mock baref"
    val agent = "mock agent"
    val status = "mock status"
    val sortField = "mock sort field"
    val sortOrder = "mock sort order"
    val appointedFromDate: LocalDate = LocalDate.parse("2020-01-01")
    val appointedToDate: LocalDate = LocalDate.parse("2020-03-01")
    val clientName = "ABC-LTD"

    val getMyOrganisationSearchParams: GetMyOrganisationPropertyLinksParameters =
      GetMyOrganisationPropertyLinksParameters(
        address = Some(address),
        uarn = Some(uarn),
        baref = Some(baref),
        agent = Some(agent),
        client = None,
        status = Some(status),
        Some(sortField),
        Some(sortOrder)
      )

    val getMyClientsSearchParams: GetMyClientsPropertyLinkParameters = GetMyClientsPropertyLinkParameters(
      address = Some(address),
      baref = Some(baref),
      client = Some(agent),
      status = Some(status),
      Some(sortField),
      Some(sortOrder),
      representationStatus = None,
      Some(appointedFromDate),
      Some(appointedToDate)
    )

    val voaApiUrl = "http://voa-modernised-api/external-property-link-management-api"
    val agentAuthorisationsUrl = s"$voaApiUrl/my-organisation/agents/{agentCode}/property-links"
    val agentAvailableAuthorisationsUrl = s"$voaApiUrl/my-organisation/agents/{agentCode}/available-property-links"
    val ownerAuthorisationUrl = s"$voaApiUrl/my-organisation/property-links/{propertyLinkId}"
    val ownerAuthorisationsUrl = s"$voaApiUrl/my-organisation/property-links"
    val clientAuthorisationUrl = s"$voaApiUrl/my-organisation/clients/all/property-links/{propertyLinkId}"
    val clientAuthorisationsUrl = s"$voaApiUrl/my-organisation/clients/all/property-links"
    val myClientPropertyLinksUrl = s"$voaApiUrl/my-organisation/clients/{clientId}/property-links"
    val createPropertyLinkUrl = s"$voaApiUrl/my-organisation/property-links"
    val createPropertyLinkOnClientBehalfUrl = s"$voaApiUrl/my-organisation/clients/{clientId}/property-links"
    val myOrganisationsAgentsUrl = s"$voaApiUrl/my-organisation/agents"
    val myClientsUrl = s"$voaApiUrl/my-organisation/clients"
    val revokeClientsPropertyLinkUrl =
      s"$voaApiUrl/my-organisation/clients/all/property-links/{submissionId}/appointment"
    val httpstring = "VoaAuthedBackendHttp"

    val connector = new ExternalPropertyLinkApi(
      httpClient = mock[VoaHttpClient],
      myAgentPropertyLinksUrl = agentAuthorisationsUrl,
      myAgentAvailablePropertyLinks = agentAvailableAuthorisationsUrl,
      myOrganisationsPropertyLinksUrl = ownerAuthorisationsUrl,
      myOrganisationsPropertyLinkUrl = ownerAuthorisationUrl,
      myClientsPropertyLinkUrl = clientAuthorisationUrl,
      myClientPropertyLinksUrl = myClientPropertyLinksUrl,
      myClientsPropertyLinksUrl = clientAuthorisationsUrl,
      createPropertyLinkUrl = createPropertyLinkUrl,
      createPropertyLinkOnClientBehalfUrl = createPropertyLinkOnClientBehalfUrl,
      myOrganisationsAgentsUrl = myOrganisationsAgentsUrl,
      revokeClientsPropertyLinkUrl = revokeClientsPropertyLinkUrl,
      myClientsUrl = myClientsUrl
    )

    val paginationParams: PaginationParams = PaginationParams(1, 1, true)
    val queryParams: Seq[(String, String)] = Seq(
      ("start", paginationParams.startPoint.toString),
      ("size", paginationParams.pageSize.toString),
      ("requestTotalRowCount", "true")
    )

    when(mockAppConfig.proxyEnabled).thenReturn(false)
    when(mockAppConfig.apimSubscriptionKeyValue).thenReturn("subscriptionId")
    when(mockAppConfig.voaApiBaseUrl).thenReturn("http://some/url/voa")
    when(mockServicesConfig.baseUrl(any())).thenReturn("http://localhost:9949/")
  }

  def encodeParams(params: Seq[(String, String)]): String =
    params
      .map { case (k, v) =>
        s"${URLEncoder.encode(k, StandardCharsets.UTF_8)}=${URLEncoder.encode(v, StandardCharsets.UTF_8)}"
      }
      .mkString("&")

  "get my organisations property links" should {

    "build the correct query params and call the modernised layer" in new Setup {

      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithAgents](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, params = Some(paginationParams))
        .futureValue shouldBe mockReturnedPropertyLinks

      val getMyOrgPropertyLinksQueryParams =
        queryParams :+ ("address" -> address) :+ ("uarn" -> uarn.toString) :+ ("baref" -> baref) :+ ("agent" -> agent) :+ ("status" -> status) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder)

      val params = encodeParams(getMyOrgPropertyLinksQueryParams)

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(s"$ownerAuthorisationsUrl?$params"))(any(), any(), any(), any())
    }

  }

  "get my agent property links" should {

    "build the correct query params and call the modernised layer" in new Setup {
      val agentCode = 1
      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithAgents](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, params = paginationParams)
        .futureValue shouldBe mockReturnedPropertyLinks

      val getMyAgentPropertyLinksQueryParams =
        queryParams :+ ("address" -> address) :+ ("uarn" -> uarn.toString) :+ ("baref" -> baref) :+ ("agent" -> agent) :+ ("status" -> status) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder)

      val params = encodeParams(getMyAgentPropertyLinksQueryParams)

      verify(connector.httpClient)
        .getWithGGHeaders(
          mEq(s"${agentAuthorisationsUrl.replace("{agentCode}", agentCode.toString)}?$params")
        )(any(), any(), any(), any())
    }

  }

  "get my agent available property links" should {

    "build the correct query params and call the modernised layer" in new Setup {
      val agentCode = 1
      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithAgents](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, params = Some(paginationParams))
        .futureValue shouldBe mockReturnedPropertyLinks

      val getMyAgentPropertyLinksQueryParams =
        queryParams :+ ("address" -> address) :+ ("agent" -> agent) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder)

      val params = encodeParams(getMyAgentPropertyLinksQueryParams)

      verify(connector.httpClient)
        .getWithGGHeaders(
          mEq(s"${agentAvailableAuthorisationsUrl.replace("{agentCode}", agentCode.toString)}?$params")
        )(any(), any(), any(), any())
    }

    "build the correct query params and call the modernised layer - when agent not provided" in new Setup {
      val agentCode = 1
      val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithAgents](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getMyAgentAvailablePropertyLinks(
          agentCode,
          getMyOrganisationSearchParams.copy(agent = None),
          params = Some(paginationParams)
        )
        .futureValue shouldBe mockReturnedPropertyLinks

      val getMyAgentPropertyLinksQueryParams =
        queryParams :+ ("address" -> address) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder)

      val params = encodeParams(getMyAgentPropertyLinksQueryParams)

      verify(connector.httpClient)
        .getWithGGHeaders(
          mEq(s"${agentAvailableAuthorisationsUrl.replace("{agentCode}", agentCode.toString)}?$params")
        )(any(), any(), any(), any())
    }
  }

  "get my organisations single property link with submissionId" should {

    "build the correct url and calls the modernised layer" in new Setup {

      val mockReturnedPropertyLink: PropertyLinkWithAgents = mock[PropertyLinkWithAgents]

      when(connector.httpClient.getWithGGHeaders[Option[PropertyLinkWithAgents]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(mockReturnedPropertyLink)))

      connector.getMyOrganisationsPropertyLink("PL1").futureValue shouldBe Some(mockReturnedPropertyLink)

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(ownerAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
    }
  }

  "get clients property links" should {

    "build the correct query params and call the modernised layer" in new Setup {

      val mockReturnedPropertyLinks: PropertyLinksWithClient = mock[PropertyLinksWithClient]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithClient](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      connector
        .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
        .futureValue shouldBe mockReturnedPropertyLinks

      val clientQueryParams =
        queryParams :+ ("address" -> address) :+ ("baref" -> baref) :+ ("client" -> agent) :+ ("status" -> status) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder) :+ ("appointedFromDate" -> appointedFromDate.toString) :+ ("appointedToDate" -> appointedToDate.toString)

      val params = encodeParams(clientQueryParams)

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(s"$clientAuthorisationsUrl?$params"))(any(), any(), any(), any())
    }

  }

  "get a client's property links" should {

    "build the correct query params and call the modernised layer" in new Setup {

      val mockReturnedPropertyLinks: PropertyLinksWithClient = mock[PropertyLinksWithClient]

      when(connector.httpClient.getWithGGHeaders[PropertyLinksWithClient](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLinks))

      private val clientOrgId = 111L
      connector
        .getClientPropertyLinks(
          clientOrgId,
          GetClientPropertyLinksParameters(
            address = Some(address),
            baref = Some(baref),
            status = Some(status),
            sortField = Some(sortField),
            sortOrder = Some(sortOrder),
            appointedFromDate = Some(appointedFromDate),
            appointedToDate = Some(appointedToDate),
            uarn = Some(uarn),
            client = Some(clientName)
          ),
          Some(paginationParams)
        )
        .futureValue shouldBe mockReturnedPropertyLinks

      val clientQueryParams =
        queryParams :+ ("address" -> address) :+ ("baref" -> baref) :+ ("status" -> status) :+ ("sortfield" -> sortField) :+ ("sortorder" -> sortOrder) :+ ("appointedFromDate" -> appointedFromDate.toString) :+ ("appointedToDate" -> appointedToDate.toString) :+ ("uarn" -> uarn.toString) :+ ("client" -> clientName)

      val params = encodeParams(clientQueryParams)

      verify(connector.httpClient).getWithGGHeaders(
        mEq(s"${myClientPropertyLinksUrl.replace("{clientId}", clientOrgId.toString)}?$params")
      )(any(), any(), any(), any())
    }

  }

  "get clients single property link with submissionId" should {

    "build the correct url and calls the modernised layer" in new Setup {

      val mockReturnedPropertyLink: ClientPropertyLink = mock[ClientPropertyLink]

      when(connector.httpClient.getWithGGHeaders[ClientPropertyLink](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedPropertyLink))

      connector.getClientsPropertyLink("PL1").futureValue shouldBe mockReturnedPropertyLink

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(clientAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
    }
  }

  "create property link" should {

    "call modernised createPropertyLink endpoint" in new Setup {

      val mockHttpResponse: HttpResponse = mock[HttpResponse]
      val mockVoaCreatePropertyLink: CreatePropertyLink = mock[CreatePropertyLink]

      when(mockVoaCreatePropertyLink.PLsubmissionId).thenReturn("PL123")

      when(
        connector.httpClient
          .postWithGgHeaders[HttpResponse](any(), any())(any(), any(), any(), any())
      ).thenReturn(Future.successful(mockHttpResponse))

      connector.createPropertyLink(testCreatePropertyLink).futureValue shouldBe mockHttpResponse

      verify(connector.httpClient)
        .postWithGgHeaders(mEq(createPropertyLinkUrl), mEq(Json.toJsObject(testCreatePropertyLink)))(
          any(),
          any(),
          any(),
          any()
        )
    }

  }
  "create property link on client behalf" should {

    "call modernised createPropertyLinkOnClientBehalf endpoint" in new Setup {

      val mockHttpResponse: HttpResponse = mock[HttpResponse]
      val mockVoaCreatePropertyLink: CreatePropertyLinkOnClientBehalf = mock[CreatePropertyLinkOnClientBehalf]
      val clientId = 100

      val date: String = "2018-09-05"
      val localDate: LocalDate = LocalDate.parse(date)

      val createPropertyLink: CreatePropertyLinkOnClientBehalf = CreatePropertyLinkOnClientBehalf(
        uarn = clientId,
        capacity = Capacity.withName("OWNER"),
        startDate = localDate,
        endDate = Some(localDate),
        method = ProvidedEvidence.withName("RATES_BILL"),
        propertyLinkSubmissionId = "44444",
        createDatetime = LocalDateTime.parse("2007-12-03T10:15:30"),
        evidence = Seq(Evidence("FILE_NAME", EvidenceType.RATES_BILL)),
        submissionSource = "DFE_UI"
      )

      when(
        connector.httpClient
          .postWithGgHeaders[HttpResponse](any(), any())(any(), any(), any(), any())
      ).thenReturn(Future.successful(mockHttpResponse))
      connector.createOnClientBehalf(createPropertyLink, clientId).futureValue shouldBe mockHttpResponse

      verify(connector.httpClient)
        .postWithGgHeaders(
          mEq(createPropertyLinkOnClientBehalfUrl.replace("{clientId}", clientId.toString)),
          mEq(Json.toJsObject(createPropertyLink))
        )(any(), any(), any(), any())
    }

  }
  "get my organisations agents" should {

    "return the agents list for the given organisation" in new Setup {

      val mockReturnedAgentList: AgentList = mock[AgentList]

      when(connector.httpClient.getWithGGHeaders[AgentList](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedAgentList))

      connector
        .getMyOrganisationsAgents()
        .futureValue shouldBe mockReturnedAgentList

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(s"$myOrganisationsAgentsUrl?requestTotalRowCount=true"))(any(), any(), any(), any())
    }

  }

  "get my clients" should {

    "return the clients for the given agent organisation" in new Setup {

      val mockReturnedClientsResponse: ClientsResponse = mock[ClientsResponse]

      when(connector.httpClient.getWithGGHeaders[ClientsResponse](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockReturnedClientsResponse))

      connector
        .getMyClients(GetClientsParameters(), None)
        .futureValue shouldBe mockReturnedClientsResponse

      verify(connector.httpClient)
        .getWithGGHeaders(mEq(myClientsUrl))(any(), any(), any(), any())
    }

  }

  "revoke client property" should {

    "revoke a client property" in new Setup {

      val mockHttpResponse: HttpResponse = mock[HttpResponse]

      when(connector.httpClient.deleteWithGgHeaders[HttpResponse](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      connector
        .revokeClientProperty("some-submissionId")
        .futureValue shouldBe ((): Unit)

      verify(connector.httpClient)
        .deleteWithGgHeaders(any())(any(), any(), any(), any())
    }

  }

}
