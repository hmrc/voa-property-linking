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
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}

import scala.concurrent.Future

class PropertyLinkingServiceSpec extends BaseUnitSpec {

  val httpResponse: HttpResponse = emptyJsonHttpResponse(200)

  val service = new PropertyLinkingService(
    modernisedPropertyLinksConnector = mockModernisedExternalPropertyLinkApi,
    propertyLinksConnector = mockPropertyLinkApi,
    featureSwitch = mockFeatureSwitch
  )

  val validPropertiesView: PropertiesView =
    PropertiesView(
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
          ))),
      client = None
    )

  val clientValidPropertiesView: PropertiesView =
    PropertiesView(
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
      agents = Some(Nil),
      client = None
    )

  val propertyLinkWithAgents: PropertyLinkWithAgents =
    PropertyLinkWithAgents(
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

  val summaryPropertyLinkWithAgents: SummaryPropertyLinkWithAgents =
    SummaryPropertyLinkWithAgents(
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
  val ownerPropertyLink: OwnerPropertyLink = OwnerPropertyLink(propertyLinkWithAgents)

  val propertyLinkClient: ModernisedPropertyLinkWithClient =
    ModernisedPropertyLinkWithClient(
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

  val summaryPropertyLinkClient: SummaryPropertyLinkWithClient =
    SummaryPropertyLinkWithClient(
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

  val clientPropertyLink: ClientPropertyLink = ClientPropertyLink(propertyLinkClient)

  val valuationHistoryResponse: ValuationHistoryResponse =
    ValuationHistoryResponse(
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
          listType = ListType.CURRENT,
          allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION)
        )))

  val agentAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val agentSummaryAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val ownerAuthResultClient: PropertyLinksWithClients =
    PropertyLinksWithClients(
      start = 1,
      size = 1,
      filterTotal = 1,
      total = 1,
      authorisations = Seq(PropertyLinkWithClient.apply(summaryPropertyLinkClient)))

  val ownerAuthorisationAgent: OwnerAuthorisation = OwnerAuthorisation(summaryPropertyLinkWithAgents)
  val ownerAuthResultAgent: OwnerAuthResult = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationAgent))

  val propertyLinksWithClient: PropertyLinksWithClient =
    PropertyLinksWithClient(1, 1, 1, 1, Seq(summaryPropertyLinkClient))
  val propertyLinksWithAgents: PropertyLinksWithAgents =
    PropertyLinksWithAgents(1, 1, 1, 1, Seq(summaryPropertyLinkWithAgents))
  val propertyLinksCount = 1

  val getMyOrganisationSearchParams: GetMyOrganisationPropertyLinksParameters =
    GetMyOrganisationPropertyLinksParameters()
  val getMyClientsSearchParams: GetMyClientsPropertyLinkParameters = GetMyClientsPropertyLinkParameters()
  val getClientSearchParams: GetClientPropertyLinksParameters = GetClientPropertyLinksParameters()
  val paginationParams: PaginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)

  "If the bstDownstream feature switch is disabled" when {
    "getMyOrganisationsPropertyLink" should {
      "call connector and return a properties view for a valid authorisation id containing valid status" in {

        when(mockModernisedExternalPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
          .thenReturn(Future.successful(Some(ownerPropertyLink)))

        service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe Some(
          PropertiesView(ownerPropertyLink.authorisation, Nil))

        verify(mockModernisedExternalPropertyLinkApi).getMyOrganisationsPropertyLink("11111")

      }

      "return none when nothing is return from connector" in {
        when(mockModernisedExternalPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
          .thenReturn(Future.successful(None))

        service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe None

        verify(mockModernisedExternalPropertyLinkApi).getMyOrganisationsPropertyLink("11111")
      }
    }

    "getClientsPropertyLink" should {
      "call connector and return a properties view for a valid authorisation id containing valid status" in {
        when(mockModernisedExternalPropertyLinkApi.getClientsPropertyLink("11111"))
          .thenReturn(Future.successful(Some(clientPropertyLink)))

        service.getClientsPropertyLink("11111").value.futureValue shouldBe Some(clientPropertyLink)

        verify(mockModernisedExternalPropertyLinkApi).getClientsPropertyLink("11111")
      }

      "return none when nothing is return from connector" in {
        when(mockModernisedExternalPropertyLinkApi.getClientsPropertyLink("11111"))
          .thenReturn(Future.successful(None))

        service.getClientsPropertyLink("11111").value.futureValue shouldBe None

        verify(mockModernisedExternalPropertyLinkApi).getClientsPropertyLink("11111")

      }
    }

    "create" should {
      "call create connector method with correct params" in {
        when(mockModernisedExternalPropertyLinkApi.createPropertyLink(any())(any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val response = service.create(apiPropertyLinkRequest).futureValue

        response shouldBe mockHttpResponse

        verify(mockModernisedExternalPropertyLinkApi).createPropertyLink(any())(any(), any())
      }
    }

    "create on client behalf" should {
      "call create connector method with correct params" in {
        val clientId = 100
        when(mockModernisedExternalPropertyLinkApi.createOnClientBehalf(any(), any())(any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val response = service.createOnClientBehalf(apiPropertyLinkRequest, clientId).futureValue

        response shouldBe mockHttpResponse

        verify(mockModernisedExternalPropertyLinkApi).createOnClientBehalf(any(), any())(any(), any())
      }
    }

    "getClientsPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {

        when(
          mockModernisedExternalPropertyLinkApi
            .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(Some(propertyLinksWithClient)))

        val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

        result.getOrElse("None returned") shouldBe ownerAuthResultClient

        verify(mockModernisedExternalPropertyLinkApi)
          .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
      }

      "return none when nothing is returned from connector" in {

        when(
          mockModernisedExternalPropertyLinkApi
            .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(None))

        val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

        result shouldBe None

        verify(mockModernisedExternalPropertyLinkApi)
          .getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
      }
    }

    "getClientPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {

        val clientId = 111L

        when(
          mockModernisedExternalPropertyLinkApi
            .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(Some(propertyLinksWithClient)))

        val result =
          service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

        result.getOrElse("None returned") shouldBe ownerAuthResultClient

        verify(mockModernisedExternalPropertyLinkApi)
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
      }

      "return none when nothing is returned from connector" in {

        val clientId = 111L

        when(
          mockModernisedExternalPropertyLinkApi
            .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(None))

        val result =
          service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

        result shouldBe None

        verify(mockModernisedExternalPropertyLinkApi)
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
      }
    }

    "getMyOrganisationsPropertyLinksCount" should {
      "call connector and return a properties count for a valid authorisation id" in {

        when(
          mockModernisedExternalPropertyLinkApi
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
          mockModernisedExternalPropertyLinkApi
            .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
      }

    }

    "getMyAgentPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {
        val agentCode = 1
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
      }

    }

    "getMyAgentAvailablePropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {
        val agentCode = 1
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
      }

    }

    "getMyOrganisationsAgents" should {
      "call connector and return the list of agents belonging to that organisation" in {
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyOrganisationsAgents())
          .thenReturn(Future.successful(organisationsAgentsList))

        val result = service
          .getMyOrganisationsAgents()
          .futureValue

        result shouldBe organisationsAgentsList

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyOrganisationsAgents()
      }

      "return none when nothing is returned from connector" in {
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyOrganisationsAgents())
          .thenReturn(Future.successful(emptyOrganisationsAgentsList))

        val result = service
          .getMyOrganisationsAgents()
          .futureValue

        result shouldBe emptyOrganisationsAgentsList

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyOrganisationsAgents()

      }
    }

    "getMyClients" should {
      "call connector and return the list of clients belonging to that agent organisation" in {
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyClients(any(), any())(any()))
          .thenReturn(Future.successful(clientsList))

        val result = service
          .getMyClients(
            GetClientsParameters(),
            Some(PaginationParams(startPoint = 1, pageSize = 15, requestTotalRowCount = true)))
          .futureValue

        result shouldBe clientsList

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyClients(any(), any())(any())
      }

      "return none when nothing is returned from connector" in {
        when(
          mockModernisedExternalPropertyLinkApi
            .getMyClients(any(), any())(any()))
          .thenReturn(Future.successful(emptyClientsList))

        val result = service
          .getMyClients(
            GetClientsParameters(),
            Some(PaginationParams(startPoint = 1, pageSize = 15, requestTotalRowCount = true)))
          .futureValue

        result shouldBe emptyClientsList

        verify(mockModernisedExternalPropertyLinkApi)
          .getMyClients(any(), any())(any())
      }
    }
  }

  "If the bstDownstream feature switch is enabled" when {
    "getMyOrganisationsPropertyLink" should {
      "call connector and return a properties view for a valid authorisation id containing valid status" in {

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
          .thenReturn(Future.successful(Some(ownerPropertyLink)))

        service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe Some(
          PropertiesView(ownerPropertyLink.authorisation, Nil))

        verify(mockPropertyLinkApi).getMyOrganisationsPropertyLink("11111")

      }

      "return none when nothing is return from connector" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
          .thenReturn(Future.successful(None))

        service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe None

        verify(mockPropertyLinkApi).getMyOrganisationsPropertyLink("11111")
      }
    }

    "getClientsPropertyLink" should {
      "call connector and return a properties view for a valid authorisation id containing valid status" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getClientsPropertyLink("11111"))
          .thenReturn(Future.successful(Some(clientPropertyLink)))

        service.getClientsPropertyLink("11111").value.futureValue shouldBe Some(clientPropertyLink)

        verify(mockPropertyLinkApi).getClientsPropertyLink("11111")
      }

      "return none when nothing is return from connector" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getClientsPropertyLink("11111"))
          .thenReturn(Future.successful(None))

        service.getClientsPropertyLink("11111").value.futureValue shouldBe None

        verify(mockPropertyLinkApi).getClientsPropertyLink("11111")

      }
    }

    "create" should {
      "call create connector method with correct params" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.createPropertyLink(any())(any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val response = service.create(apiPropertyLinkRequest).futureValue

        response shouldBe mockHttpResponse

        verify(mockPropertyLinkApi).createPropertyLink(any())(any(), any())
      }
    }

    "create on client behalf" should {
      "call create connector method with correct params" in {
        val clientId = 100
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.createOnClientBehalf(any(), any())(any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        val response = service.createOnClientBehalf(apiPropertyLinkRequest, clientId).futureValue

        response shouldBe mockHttpResponse

        verify(mockPropertyLinkApi).createOnClientBehalf(any(), any())(any(), any())
      }
    }

    "getClientsPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(Some(propertyLinksWithClient)))

        val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

        result.getOrElse("None returned") shouldBe ownerAuthResultClient

        verify(mockPropertyLinkApi).getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
      }

      "return none when nothing is returned from connector" in {

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(None))

        val result = service.getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams)).value.futureValue

        result shouldBe None

        verify(mockPropertyLinkApi).getClientsPropertyLinks(getMyClientsSearchParams, Some(paginationParams))
      }
    }

    "getClientPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {

        val clientId = 111L

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(Some(propertyLinksWithClient)))

        val result =
          service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

        result.getOrElse("None returned") shouldBe ownerAuthResultClient

        verify(mockPropertyLinkApi)
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
      }

      "return none when nothing is returned from connector" in {

        val clientId = 111L

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(None))

        val result =
          service.getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams)).value.futureValue

        result shouldBe None

        verify(mockPropertyLinkApi)
          .getClientPropertyLinks(clientId, getClientSearchParams, Some(paginationParams))
      }
    }

    "getMyOrganisationsPropertyLinksCount" should {
      "call connector and return a properties count for a valid authorisation id" in {

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
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

        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockPropertyLinkApi)
          .getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
      }
    }

    "getMyAgentPropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {
        val agentCode = 1
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockPropertyLinkApi)
          .getMyAgentPropertyLinks(agentCode, getMyOrganisationSearchParams, paginationParams)
      }

    }

    "getMyAgentAvailablePropertyLinks" should {
      "call connector and return a Owner Auth Result for a valid authorisation id" in {
        val agentCode = 1
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams)))
          .thenReturn(Future.successful(propertyLinksWithAgents))

        val result = service
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
          .futureValue

        result shouldBe ownerAuthResultAgent

        verify(mockPropertyLinkApi)
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationSearchParams, Some(paginationParams))
      }
    }

    "getMyOrganisationsAgents" should {
      "call connector and return the list of agents belonging to that organisation" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyOrganisationsAgents())
          .thenReturn(Future.successful(organisationsAgentsList))

        val result = service
          .getMyOrganisationsAgents()
          .futureValue

        result shouldBe organisationsAgentsList

        verify(mockPropertyLinkApi)
          .getMyOrganisationsAgents()
      }

      "return none when nothing is returned from connector" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyOrganisationsAgents())
          .thenReturn(Future.successful(emptyOrganisationsAgentsList))

        val result = service
          .getMyOrganisationsAgents()
          .futureValue

        result shouldBe emptyOrganisationsAgentsList

        verify(mockPropertyLinkApi)
          .getMyOrganisationsAgents()
      }
    }

    "getMyClients" should {
      "call connector and return the list of clients belonging to that agent organisation" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyClients(any(), any())(any()))
          .thenReturn(Future.successful(clientsList))

        val result = service
          .getMyClients(
            GetClientsParameters(),
            Some(PaginationParams(startPoint = 1, pageSize = 15, requestTotalRowCount = true)))
          .futureValue

        result shouldBe clientsList

        verify(mockPropertyLinkApi)
          .getMyClients(any(), any())(any())
      }

      "return none when nothing is returned from connector" in {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(
          mockPropertyLinkApi
            .getMyClients(any(), any())(any()))
          .thenReturn(Future.successful(emptyClientsList))

        val result = service
          .getMyClients(
            GetClientsParameters(),
            Some(PaginationParams(startPoint = 1, pageSize = 15, requestTotalRowCount = true)))
          .futureValue

        result shouldBe emptyClientsList

        verify(mockPropertyLinkApi)
          .getMyClients(any(), any())(any())
      }
    }
  }
}
