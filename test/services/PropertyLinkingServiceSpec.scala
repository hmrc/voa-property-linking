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

package services

import basespecs.BaseUnitSpec
import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import models._
import models.mdtp.propertylink.myclients.{PropertyLinkWithClient, PropertyLinksWithClients}
import models.modernised._
import models.modernised.externalpropertylink.myclients.{PropertyLinkWithClient => ModernisedPropertyLinkWithClient, _}
import models.modernised.externalpropertylink.myorganisations._
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future

class PropertyLinkingServiceSpec extends BaseUnitSpec {

  val httpResponse = HttpResponse(200)

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
    NDRListValuationHistoryItems = Seq(APIValuationHistory(
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
    parties = Seq(APIParty(id = 24680,
      authorisedPartyStatus = "APPROVED",
      authorisedPartyOrganisationId = 123456,
      permissions = Seq(Permissions(
        id = 24680,
        checkPermission = "START_AND_CONTINUE",
        challengePermission = "NOT_PERMITTED",
        endDate = None)))),
    agents = Some(Seq(LegacyParty(
      authorisedPartyId = 24680,
      agentCode = 1111,
      organisationName = "org name",
      organisationId = 123456,
      checkPermission = "START_AND_CONTINUE",
      challengePermission = "NOT_PERMITTED"
    ))))

  val clientValidPropertiesView = PropertiesView(
    authorisationId = 11111,
    uarn = 33333,
    address = Some("1 HIGH STREET, BRIGHTON"),
    authorisationStatus = "APPROVED",
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    NDRListValuationHistoryItems = Seq(APIValuationHistory(
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

  val propertyLinkWithAgents: PropertyLinkWithAgents = PropertyLinkWithAgents(authorisationId = 11111,
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
        status = "APPROVED",
        representationSubmissionId = "",
        representativeCode = 1111,
        checkPermission = "START_AND_CONTINUE",
        challengePermission = "NOT_PERMITTED"))
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
        status = "APPROVED",
        representationSubmissionId = "",
        representativeCode = 1111,
        checkPermission = "START_AND_CONTINUE",
        challengePermission = "NOT_PERMITTED"))
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
    client = ClientDetails(55555, "mock org"),
    representationStatus = "APPROVED")

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
    client = ClientDetails(55555, "mock org"),
    representationStatus = "APPROVED")

  val clientPropertyLink = ClientPropertyLink(
    propertyLinkClient
  )

  val valuationHistoryResponse = ValuationHistoryResponse(Seq(ValuationHistory(
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
    billingAuthorityCode = None)))

  val agentAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val agentSummaryAuthorisation: ModernisedPropertyLinkWithClient = propertyLinkClient
  val ownerAuthResultClient = PropertyLinksWithClients(1, 1, 1, 1, Seq(PropertyLinkWithClient.apply(summaryPropertyLinkClient)))

  val ownerAuthorisationAgent = OwnerAuthorisation(summaryPropertyLinkWithAgents)
  val ownerAuthResultAgent = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationAgent))

  val propertyLinksWithClient = PropertyLinksWithClient(1, 1, 1, 1, Seq(summaryPropertyLinkClient))
  val propertyLinksWithAgents = PropertyLinksWithAgents(1, 1, 1, 1, Seq(summaryPropertyLinkWithAgents))

  val getMyOrganisationSearchParams = GetMyOrganisationPropertyLinksParameters()
  val getMyClientsSearchParams = GetMyClientsPropertyLinkParameters()
  val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)

  "getMyOrganisationsPropertyLink" should {
    "call connector and return a properties view for a valid authorisation id containing valid status" in {

      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLink("11111"))
        .thenReturn(Future.successful(Some(ownerPropertyLink)))

      service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe Some(PropertiesView(ownerPropertyLink.authorisation, Nil))

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

      service.getClientsPropertyLink("11111").value.futureValue shouldBe Some(PropertiesView(clientPropertyLink.authorisation, Nil))

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


  "getMyOrganisationsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(Some(propertyLinksWithAgents)))

      val result = service.getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)).value.futureValue

      result.getOrElse("None returned") shouldBe ownerAuthResultAgent

      verify(mockExternalPropertyLinkApi).getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)))
        .thenReturn(Future.successful(None))

      val result = service.getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams)).value.futureValue

      result.getOrElse("None returned") shouldBe "None returned"

      verify(mockExternalPropertyLinkApi).getMyOrganisationsPropertyLinks(getMyOrganisationSearchParams, Some(paginationParams))

    }
  }

}
