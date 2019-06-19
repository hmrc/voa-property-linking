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
import connectors.{ExternalPropertyLinkConnector, ExternalPropertyLinkConnectorSpec, ExternalValuationManagementApi, PropertyLinkingConnector}
import models._
import models.modernised.PropertyLinkStatus.PropertyLinkStatus
import models.modernised.{PropertyLinksWithAgents, _}
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.mockito.ArgumentMatchers
import org.scalatest.concurrent.Eventually._
import org.mockito.ArgumentMatchers.{eq => mEq, _}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import org.scalatestplus.mockito
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Cats
import cats.instances.{FutureInstances, ListInstances, OptionInstances, VectorInstances}
import cats.syntax._
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext, Future}

class PropertyLinkingServiceSpec extends UnitSpec with MockitoSugar with WithFakeApplication with Cats {

  val mockPropertyLinkingConnector = mock[ExternalPropertyLinkConnector]
  val mockExternalValuationManagementApi = mock[ExternalValuationManagementApi]
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val httpResponse = HttpResponse(200)
  implicit val fakeHc = HeaderCarrier()
  implicit val modernisedEnrichedRequest = ModernisedEnrichedRequest(FakeRequest(), "XXXXX", "YYYYY")
  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  val date = LocalDate.parse("2018-09-05")
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
  //val service :PropertyLinkingService = fakeApplication.injector.instanceOf[PropertyLinkingService]
  val service = new PropertyLinkingService(propertyLinksConnector = mockPropertyLinkingConnector, mockExternalValuationManagementApi)

  val validPropertiesView = PropertiesView(
    authorisationId = 11111,
    uarn = 33333,
    //authorisationOwnerOrganisationId = 11111,
    //authorisationOwnerPersonId = 11111,
    authorisationStatus = "APPROVED",
    //authorisationMethod = "",
    //authorisationOwnerCapacity = "",
    //createDatetime = Clock.fixed(Instant.parse("2018-04-29T10:15:30.00Z"),
      //ZoneId.of("Europe/London")).instant(), //Unknown just for testing equality atm,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    NDRListValuationHistoryItems = Seq(APIValuationHistory(
            asstRef = 125689,
            listYear = "2017",
            uarn = 923411,
            effectiveDate = date,
            rateableValue = Some(2599),
            address = "1 HIGH STREET, BRIGHTON",
            billingAuthorityReference = "VOA1"
          )),
    parties = Seq(APIParty(id = 24680,
                authorisedPartyStatus = "APPROVED",
                authorisedPartyOrganisationId = 123456,
                permissions = Seq(Permissions(
                  id = 13579,
                  checkPermission = "APPROVED",
                  challengePermission = "APPROVED",
                  endDate = None))
  )))

  val  propertyLinkWithAgents = PropertyLinkWithAgents(authorisationId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    uarn = 33333,
    address = "mock Address",
    localAuthorityRef = "44444",
    agents = Seq(AgentDetails(authorisedPartyId = 24680,
      organisationId = 123456,
      organisationName = "org name",
      status = "APPROVED",
      representationSubmissionId = "",
      representativeCode =  111111,
      checkPermission = "APPROVED",
      challengePermission = "APPROVED"))
  )
  val ownerPropertyLink = OwnerPropertyLink(propertyLinkWithAgents)


  val propertyLinkClient = PropertyLinkWithClient(authorisationId = 11111,
    authorisedPartyId = 11111,
    status = PropertyLinkStatus.APPROVED,
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    uarn = 33333,
    address = "mock address",
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

  val ownerAuthorisationClient = OwnerAuthorisation(propertyLinkClient)
  val ownerAuthResultClient = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationClient))

  val ownerAuthorisationAgent = OwnerAuthorisation(propertyLinkWithAgents)
  val ownerAuthResultAgent = OwnerAuthResult(1, 1, 1, 1, Seq(ownerAuthorisationAgent))

  val propertyLinksWithClient =  PropertyLinksWithClient(1, 1, 1, 1, Seq(propertyLinkClient))
  val propertyLinksWithAgents = PropertyLinksWithAgents(1, 1, 1, 1, Seq(propertyLinkWithAgents))

  val searchParams = GetPropertyLinksParameters()
  val paginationParams = PaginationParams(1, 1, true)



//  "getMyOrganisationsPropertyLink" should {
//    "call connector and return a properties view for a valid authorisation id containing valid status" in {
//
//      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLink("11111")).thenReturn(Future.successful(Some(ownerPropertyLink)))
//      when(mockExternalValuationManagementApi.getValuationHistory(33333, "11111")).thenReturn(Future.successful(Some(valuationHistoryResponse)))
//
//      val result = Await.result(service.getMyOrganisationsPropertyLink("11111"), 1000 millis)// shouldBe validPropertiesView
//
//      result shouldBe Some(validPropertiesView)
//
//      //result.getOrElse("None returned") shouldBe validPropertiesView
//
////      rez.authorisationId shouldBe validPropertiesView.authorisationId
////      rez.authorisationStatus shouldBe validPropertiesView.authorisationStatus
////      rez.startDate shouldBe validPropertiesView.startDate
////      rez.endDate shouldBe validPropertiesView.endDate
////      rez.submissionId shouldBe validPropertiesView.submissionId
////      rez.uarn shouldBe validPropertiesView.uarn
//
//      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLink("11111")
//
//    }
//
//    "return none when nothing is return from connector" in {
//      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLink("11111")).thenReturn(Future.successful(None))
//
//      service.getMyOrganisationsPropertyLink("11111") shouldBe "None returned"
//
//      //result.getOrElse("None returned") shouldBe "None returned"
//
//      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLink("11111")
//    }
//  }
//
//  "getClientsPropertyLink" should {
//    "call connector and return a properties view for a valid authorisation id containing valid status" in {
//
//      when(mockPropertyLinkingConnector.getClientsPropertyLink("11111")).thenReturn(Future.successful(Some(clientPropertyLink)))
//      when(mockExternalValuationManagementApi.getValuationHistory(33333, "11111")).thenReturn(Future.successful(Some(valuationHistoryResponse)))
//
//      service.getClientsPropertyLink("11111") shouldBe validPropertiesView
//
//      //result.getOrElse("None returned") shouldBe validPropertiesView
//
//      verify(mockPropertyLinkingConnector).getClientsPropertyLink("11111")
//
//    }
//
//    "return none when nothing is return from connector" in {
//      when(mockPropertyLinkingConnector.getClientsPropertyLink("11111")).thenReturn(Future.successful(None))
//
//      service.getMyOrganisationsPropertyLink("11111") shouldBe "None returned"
//
//      //result.getOrElse("None returned") shouldBe "None returned"
//
//      verify(mockPropertyLinkingConnector).getClientsPropertyLink("11111")
//
//    }
//  }
//
//  "create" should {
//    "call create connector method with correct params" in {
//
//      val request = APIPropertyLinkRequest(
//        uarn = 11111,
//        authorisationOwnerOrganisationId = 2222,
//        authorisationOwnerPersonId = 33333,
//        createDatetime = Instant.now(),
//        authorisationMethod = "mock method",
//        uploadedFiles = Seq(),
//        submissionId = "44444",
//        authorisationOwnerCapacity = "OWNER",
//        startDate = date,
//        endDate = Some(date))
//
//      val propertyLink = CreatePropertyLink(request)
//
//      val httpResponse = HttpResponse(200)
//
//      when(mockPropertyLinkingConnector.createPropertyLink(any()) (any(), any())).thenReturn(Future.successful(mockHttpResponse))
//
//      service.create(request) shouldBe Future.successful(mockHttpResponse)
//
//      verify(mockPropertyLinkingConnector).createPropertyLink(any())
//    }
//  }

  "getClientsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockPropertyLinkingConnector.getClientsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(Some(propertyLinksWithClient)))

      val result = service.getClientsPropertyLinks(searchParams, Some(paginationParams))

      result.getOrElse("None returned") shouldBe ownerAuthResultClient

      verify(mockPropertyLinkingConnector).getClientsPropertyLinks(searchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockPropertyLinkingConnector.getClientsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(None))

      val result = service.getClientsPropertyLinks(searchParams, Some(paginationParams))

      result.getOrElse("None returned") shouldBe "None returned"

      //verify(mockPropertyLinkingConnector).getClientsPropertyLinks(searchParams, Some(paginationParams))

    }
  }


  "getMyOrganisationsPropertyLinks" should {
    "call connector and return a Owner Auth Result for a valid authorisation id" in {

      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(Some(propertyLinksWithAgents)))

      val result = service.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))

      result.getOrElse("None returned") shouldBe ownerAuthResultAgent

      verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))
    }

    "return none when nothing is returned from connector" in {

      when(mockPropertyLinkingConnector.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))).thenReturn(Future.successful(None))

      val result = service.getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))

      result.getOrElse("None returned") shouldBe "None returned"

      //verify(mockPropertyLinkingConnector).getMyOrganisationsPropertyLinks(searchParams, Some(paginationParams))

    }
  }

}
