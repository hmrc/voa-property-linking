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

package uk.gov.hmrc.voapropertylinking.services

import basespecs.BaseUnitSpec
import models._
import models.mdtp.propertylink.myclients.{PropertyLinkWithClient, PropertyLinksWithClients}
import models.mdtp.propertylink.projections.{OwnerAuthResult, OwnerAuthorisation}
import models.modernised._
import models.modernised.externalpropertylink.myclients.{PropertyLinkWithClient => ModernisedPropertyLinkWithClient, _}
import models.modernised.externalpropertylink.myorganisations._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}

import scala.concurrent.Future

class PropertyLinkingServiceSpec extends BaseUnitSpec {

  val httpResponse = emptyJsonHttpResponse(200)

  val service = new PropertyLinkingService(
    propertyLinksConnector = mockExternalPropertyLinkApi,
    mockExternalValuationManagementApi
  )

  val validPropertiesView = PropertiesView(
    authorisationId = 11111,
    uarn = 33333,
    address = Some("1 HIGH STREET, BRIGHTON"),
    authorisationStatus = "APPROVED",
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    NDRListValuationHistoryItems = Seq(
      APIValuationHistory(
        asstRef = 125689,
        listYear = "2017",
        uarn = 923411,
        effectiveDate = Some(date),
        rateableValue = Some(2599),
        address = "1 HIGH STREET, BRIGHTON",
        billingAuthorityReference = "VOA1",
        currentFromDate = None,
        currentToDate = None
      )),
    parties = Seq(
      APIParty(
        id = 24680,
        authorisedPartyOrganisationId = 123456
      )),
    agents = Some(
      Seq(
        LegacyParty(
          authorisedPartyId = 24680,
          agentCode = 1111,
          organisationName = "org name",
          organisationId = 123456
        )))
  )

  val clientValidPropertiesView = PropertiesView(
    authorisationId = 11111,
    uarn = 33333,
    address = Some("1 HIGH STREET, BRIGHTON"),
    authorisationStatus = "APPROVED",
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    NDRListValuationHistoryItems = Seq(
      APIValuationHistory(
        asstRef = 125689,
        listYear = "2017",
        uarn = 923411,
        effectiveDate = Some(date),
        rateableValue = Some(2599),
        address = "1 HIGH STREET, BRIGHTON",
        billingAuthorityReference = "VOA1",
        currentFromDate = None,
        currentToDate = None
      )),
    parties = Seq(),
    agents = Some(Nil)
  )

  val propertyLinkWithAgents: PropertyLinkWithAgents = PropertyLinkWithAgents(
    authorisationId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    capacity = "OWNER",
    uarn = 33333,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "44444",
    agents = Seq(
      AgentDetails(
        authorisedPartyId = 24680,
        organisationId = 123456,
        organisationName = "org name",
        representativeCode = 1111
      ))
  )

  val summaryPropertyLinkWithAgents: SummaryPropertyLinkWithAgents = SummaryPropertyLinkWithAgents(
    authorisationId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    uarn = 33333,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "44444",
    agents = Seq(
      AgentDetails(
        authorisedPartyId = 24680,
        organisationId = 123456,
        organisationName = "org name",
        representativeCode = 1111
      ))
  )
  val ownerPropertyLink = OwnerPropertyLink(propertyLinkWithAgents)

  val propertyLinkClient = ModernisedPropertyLinkWithClient(
    authorisationId = 11111,
    authorisedPartyId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    capacity = "OWNER",
    uarn = 33333,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "44444",
    client = ClientDetails(55555, "mock org")
  )

  val summaryPropertyLinkClient = SummaryPropertyLinkWithClient(
    authorisationId = 11111,
    authorisedPartyId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    uarn = 33333,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "44444",
    appointedDate = date,
    client = ClientDetails(55555, "mock org")
  )

  val clientPropertyLink = ClientPropertyLink(
    propertyLinkClient
  )

  val valuationHistoryResponse = ValuationHistoryResponse(
    Seq(
      ValuationHistory(
        asstRef = 125689,
        listYear = "2017",
        uarn = 923411,
        billingAuthorityReference = "VOA1",
        address = "1 HIGH STREET, BRIGHTON",
        description = None,
        specialCategoryCode = None,
        compositeProperty = None,
        effectiveDate = Some(date),
        listAlterationDate = None,
        numberOfPreviousProposals = None,
        settlementCode = None,
        totalAreaM2 = None,
        costPerM2 = None,
        rateableValue = Some(2599),
        transitionalCertificate = None,
        deletedIndicator = None,
        valuationDetailsAvailable = None,
        billingAuthCode = None,
        listType = ListType.CURRENT
      )))

  val agentAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val agentSummaryAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val ownerAuthResultClient =
    PropertyLinksWithClients(1, 1, 1, 1, Seq(PropertyLinkWithClient.apply(summaryPropertyLinkClient)))

  val ownerAuthorisationAgent = OwnerAuthorisation(summaryPropertyLinkWithAgents)
  val ownerAuthResultAgent = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationAgent))

  val propertyLinksWithClient = PropertyLinksWithClient(1, 1, 1, 1, Seq(summaryPropertyLinkClient))
  val propertyLinksWithAgents = PropertyLinksWithAgents(1, 1, 1, 1, Seq(summaryPropertyLinkWithAgents))
  val propertyLinksCount = 1

  val getMyOrganisationSearchParams = GetMyOrganisationPropertyLinksParameters()
  val getMyClientsSearchParams = GetMyClientsPropertyLinkParameters()
  val getClientSearchParams = GetClientPropertyLinksParameters()
  val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)

  "getMyOrganisationsPropertyLink" should {
    "call connector and return a properties view for a valid authorisation id containing valid status" in {

      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
        .thenReturn(Future.successful(Some(ownerPropertyLink)))

      service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe Some(
        PropertiesView(ownerPropertyLink.authorisation, Nil))

      verify(mockExternalPropertyLinkApi).getMyOrganisationsPropertyLink("11111")

    }

    "return none when nothing is return from connector" in {
      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
        .thenReturn(Future.successful(None))

      service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe None

      verify(mockExternalPropertyLinkApi).getMyOrganisationsPropertyLink("11111")
    }
  }

  "getClientsPropertyLink" should {
    "call connector and return a properties view for a valid authorisation id containing valid status" in {
      when(mockExternalPropertyLinkApi.getClientsPropertyLink("11111"))
        .thenReturn(Future.successful(Some(clientPropertyLink)))

      service.getClientsPropertyLink("11111").value.futureValue shouldBe Some(clientPropertyLink)

      verify(mockExternalPropertyLinkApi).getClientsPropertyLink("11111")
    }

    "return none when nothing is return from connector" in {
      when(mockExternalPropertyLinkApi.getClientsPropertyLink("11111"))
        .thenReturn(Future.successful(None))

      service.getClientsPropertyLink("11111").value.futureValue shouldBe None

      verify(mockExternalPropertyLinkApi).getClientsPropertyLink("11111")

    }
  }

  "create" should {
    "call create connector method with correct params" in {
      when(mockExternalPropertyLinkApi.createPropertyLink(any())(any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      val response = service.create(apiPropertyLinkRequest).futureValue

      response shouldBe mockHttpResponse

      verify(mockExternalPropertyLinkApi).createPropertyLink(any())(any(), any())
    }
  }

  "create on client behalf" should {
    "call create connector method with correct params" in {
      val clientId = 100
      when(mockExternalPropertyLinkApi.createOnClientBehalf(any(), any())(any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))

      val response = service.createOnClientBehalf(apiPropertyLinkRequest, clientId).futureValue

      response shouldBe mockHttpResponse

      verify(mockExternalPropertyLinkApi).createOnClientBehalf(any(), any())(any(), any())
    }
  }

  "getClientsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockExternalPropertyLinkApi.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(Some(propertyLinksWithClient)))

      val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

      result.getOrElse("None returned") shouldBe ownerAuthResultClient

      verify(mockExternalPropertyLinkApi).getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockExternalPropertyLinkApi.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(None))

      val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

      result shouldBe None

      verify(mockExternalPropertyLinkApi).getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
    }
  }

  "getClientPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      val clientId = 111L

      when(
        mockExternalPropertyLinkApi
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(Some(propertyLinksWithClient)))

      val result =
        service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

      result.getOrElse("None returned") shouldBe ownerAuthResultClient

      verify(mockExternalPropertyLinkApi)
        .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      val clientId = 111L

      when(
        mockExternalPropertyLinkApi
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(None))

      val result =
        service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

      result shouldBe None

      verify(mockExternalPropertyLinkApi)
        .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
    }
  }

  "getMyOrganisationsPropertyLinksCount" should {
    "call connector and return a properties count for a valid authorisation id" in {

      when(
        mockExternalPropertyLinkApi
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, None))
        .thenReturn(Future.successful(propertyLinksWithAgents))

      val result = service
        .getMyOrganisationsPropertyLinksCount()
        .futureValue

      result shouldBe propertyLinksCount

    }

  }

  "getMyOrganisationsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(
        mockExternalPropertyLinkApi
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(propertyLinksWithAgents))

      val result = service
        .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
        .futureValue

      result shouldBe ownerAuthResultAgent

      verify(mockExternalPropertyLinkApi)
        .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
    }

  }

  "getMyAgentPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {
      val agentCode = 1
      when(
        mockExternalPropertyLinkApi
          .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams))
        .thenReturn(Future.successful(propertyLinksWithAgents))

      val result = service
        .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
        .futureValue

      result shouldBe ownerAuthResultAgent

      verify(mockExternalPropertyLinkApi)
        .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
    }

  }

  "getMyAgentAvailablePropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {
      val agentCode = 1
      when(
        mockExternalPropertyLinkApi
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(propertyLinksWithAgents))

      val result = service
        .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
        .futureValue

      result shouldBe ownerAuthResultAgent

      verify(mockExternalPropertyLinkApi)
        .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
    }

  }

  "getMyOrganisationsAgents" should {
    "call connector and return the list of agents belonging to that organisation" in {
      when(
        mockExternalPropertyLinkApi
          .getMyOrganisationsAgents())
        .thenReturn(Future.successful(organisationsAgentsList))

      val result = service
        .getMyOrganisationsAgents()
        .futureValue

      result shouldBe organisationsAgentsList

      verify(mockExternalPropertyLinkApi)
        .getMyOrganisationsAgents()
    }

    "return none when nothing is returned from connector" in {
      when(
        mockExternalPropertyLinkApi
          .getMyOrganisationsAgents())
        .thenReturn(Future.successful(emptyOrganisationsAgentsList))

      val result = service
        .getMyOrganisationsAgents()
        .futureValue

      result shouldBe emptyOrganisationsAgentsList

      verify(mockExternalPropertyLinkApi)
        .getMyOrganisationsAgents()

    }
  }

  "getMyClients" should {
    "call connector and return the list of clients belonging to that agent organisation" in {
      when(
        mockExternalPropertyLinkApi
          .getMyClients(any(), any())(any()))
        .thenReturn(Future.successful(clientsList))

      val result = service
        .getMyClients(GetClientsParameters(), Some(PaginationParams(1, 15, true)))
        .futureValue

      result shouldBe clientsList

      verify(mockExternalPropertyLinkApi)
        .getMyClients(any(), any())(any())
    }

    "return none when nothing is returned from connector" in {
      when(
        mockExternalPropertyLinkApi
          .getMyClients(any(), any())(any()))
        .thenReturn(Future.successful(emptyClientsList))

      val result = service
        .getMyClients(GetClientsParameters(), Some(PaginationParams(1, 15, true)))
        .futureValue

      result shouldBe emptyClientsList

      verify(mockExternalPropertyLinkApi)
        .getMyClients(any(), any())(any())

    }
  }

}
