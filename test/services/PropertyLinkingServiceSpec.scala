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

import java.time._

import binders.GetPropertyLinksParameters
import connectors.authorisationsearch.PropertyLinkingConnector
import connectors.externalpropertylink.ExternalPropertyLinkConnector
import connectors.externalvaluation.ExternalValuationManagementApi
import models._
import models.mdtp.propertylink.myclients.{PropertyLinkWithClient, PropertyLinksWithClients}
import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.externalpropertylink.myorganisations.{AgentDetails, OwnerPropertyLink, PropertyLinkWithAgents, PropertyLinksWithAgents}
import models.modernised._
import models.modernised.externalpropertylink.myclients.{ClientDetails, ClientPropertyLink, PropertyLinksWithClient, PropertyLinkWithClient => ModernisedPropertyLinkWithClient}
import models.searchApi.{AgentAuthResult, AgentAuthorisation, OwnerAuthResult, OwnerAuthorisation}
import models.voa.propertylinking.requests.CreatePropertyLink
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Cats
import org.scalatest.concurrent.{Futures, ScalaFutures}

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingServiceSpec
  extends UnitSpec with BeforeAndAfterEach with MockitoSugar with WithFakeApplication with Cats with ScalaFutures {

  val mockPropertyLinkingConnector = mock[ExternalPropertyLinkConnector]
  val mockExternalValuationManagementApi = mock[ExternalValuationManagementApi]
  val mockLegacyPropertyLinkingConnector = mock[PropertyLinkingConnector]
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val httpResponse = HttpResponse(200)
  implicit val fakeHc = HeaderCarrier()
  implicit val modernisedEnrichedRequest = ModernisedEnrichedRequest(FakeRequest(), "XXXXX", "YYYYY")
  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val date = LocalDate.parse("2018-09-05")
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
  val service = new PropertyLinkingService(
    propertyLinksConnector = mockPropertyLinkingConnector,
    mockExternalValuationManagementApi,
    mockLegacyPropertyLinkingConnector)

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

  val  propertyLinkWithAgents = PropertyLinkWithAgents(authorisationId = 11111,
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
        representativeCode =  1111,
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

  val agentAuthorisation = propertyLinkClient
  val ownerAuthResultClient = PropertyLinksWithClients(1, 1, 1, 1, Seq(PropertyLinkWithClient.apply(agentAuthorisation)))

  val ownerAuthorisationAgent = OwnerAuthorisation(propertyLinkWithAgents)
  val ownerAuthResultAgent = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationAgent))

  val propertyLinksWithClient =  PropertyLinksWithClient(1, 1, 1, 1, Seq(propertyLinkClient))
  val propertyLinksWithAgents = PropertyLinksWithAgents(1, 1, 1, 1, Seq(propertyLinkWithAgents))

  val searchParams = GetPropertyLinksParameters()
  val paginationParams = PaginationParams(1, 1, true)



  "getMyOrganisationsPropertyLink" should {
    "call connector and return a properties view for a valid authorisation id containing valid status" in {

      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLink("11111")).thenReturn(Future.successful(Some(ownerPropertyLink)))
      when(mockExternalValuationManagementApi.getValuationHistory(33333, "11111")).thenReturn(Future.successful(Some(valuationHistoryResponse)))

      service.getMyOrganisationsPropertyLink("11111").value.futureValue.get shouldBe validPropertiesView

      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLink("11111")

    }

    "return none when nothing is return from connector" in {
      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLink("11111")).thenReturn(Future.successful(None))

      service.getMyOrganisationsPropertyLink("11111").value.futureValue shouldBe None

      //verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLink("11111")
    }
  }

  "getClientsPropertyLink" should {
    "call connector and return a properties view for a valid authorisation id containing valid status" in {

      when(mockPropertyLinkingConnector.getClientsPropertyLink("11111")).thenReturn(Future.successful(Some(clientPropertyLink)))
      when(mockExternalValuationManagementApi.getValuationHistory(33333, "11111")).thenReturn(Future.successful(Some(valuationHistoryResponse)))

      service.getClientsPropertyLink("11111").value.futureValue.get shouldBe clientValidPropertiesView

      //result.getOrElse("None returned") shouldBe validPropertiesView

      verify(mockPropertyLinkingConnector).getClientsPropertyLink("11111")

    }

    "return none when nothing is return from connector" in {
      when(mockPropertyLinkingConnector.getClientsPropertyLink("11111")).thenReturn(Future.successful(None))

      service.getClientsPropertyLink("11111").value.futureValue shouldBe None

      verify(mockPropertyLinkingConnector).getClientsPropertyLink("11111")

    }
  }

  "create" should {
    "call create connector method with correct params" in {

      val request = APIPropertyLinkRequest(
        uarn = 11111,
        authorisationOwnerOrganisationId = 2222,
        authorisationOwnerPersonId = 33333,
        createDatetime = Instant.now(),
        authorisationMethod = "RATES_BILL",
        uploadedFiles = Seq(),
        submissionId = "44444",
        authorisationOwnerCapacity = "OWNER",
        startDate = date,
        endDate = Some(date))

      val propertyLink = CreatePropertyLink(request)

      val httpResponse = HttpResponse(200)

      when(mockPropertyLinkingConnector.createPropertyLink(any())(any(), any())).thenReturn(Future.successful(mockHttpResponse))

      val response = service.create(request).futureValue

      response shouldBe mockHttpResponse

      verify(mockPropertyLinkingConnector).createPropertyLink(any())(any(), any())
    }
  }

  "getClientsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockPropertyLinkingConnector.getClientsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(Some(propertyLinksWithClient)))

      val result = service.getClientsPropertyLinks(searchParams, Some(paginationParams)).value

      result.getOrElse("None returned") shouldBe ownerAuthResultClient

      verify(mockPropertyLinkingConnector).getClientsPropertyLinks(searchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockPropertyLinkingConnector.getClientsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(None))

      val result = service.getClientsPropertyLinks(searchParams, Some(paginationParams)).value.futureValue

      result shouldBe None

      verify(mockPropertyLinkingConnector).getClientsPropertyLinks(searchParams, Some(paginationParams))
    }
  }


  "getMyOrganisationsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(Some(propertyLinksWithAgents)))

      val result = service.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams)).value

      result.getOrElse("None returned") shouldBe ownerAuthResultAgent

      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(None))

      val result = service.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams)).value

      result.getOrElse("None returned") shouldBe "None returned"

      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))

    }
  }

  override def beforeEach() = {
    Mockito.reset(mockPropertyLinkingConnector)
  }
}
