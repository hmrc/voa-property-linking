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

package uk.gov.hmrc.voapropertylinking.controllers

import java.time.Instant

import basespecs.BaseControllerSpec
import cats.data._
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.projections.{OwnerAuthResult, OwnerAuthorisation}
import models.mdtp.propertylink.requests.PropertyLinkRequest
import models.modernised.PropertyLinkStatus
import models.modernised.externalpropertylink.myclients.{ClientDetails, ClientPropertyLink, PropertyLinkWithClient}
import models.searchApi.{OwnerAuthResult => ModernisedOwnerAuthResult}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import utils.FakeObjects

import scala.concurrent.Future

class PropertyLinkingControllerSpec extends BaseControllerSpec with FakeObjects {

  lazy val testController = new PropertyLinkingController(
    controllerComponents = Helpers.stubControllerComponents(),
    authenticated = preAuthenticatedActionBuilders(),
    propertyLinkService = mockPropertyLinkingService,
    assessmentService = mockAssessmentService,
    auditingService = mock[AuditingService]
  )

  val clientOrgId: Long = 222L
  val agentOrgId: Long = 333L

  val propertyLinkWithClient: PropertyLinkWithClient = PropertyLinkWithClient(
    authorisationId = 11111L,
    authorisedPartyId = 1234567L,
    uarn = 33333L,
    address = "1 HIGH STREET, BRIGHTON",
    startDate = date,
    endDate = Some(date),
    submissionId = "22222",
    status = PropertyLinkStatus.APPROVED,
    capacity = "someCapacity",
    localAuthorityRef = "21323",
    client = ClientDetails(123L, "Org Name")
  )

  val clientPropertyLink: ClientPropertyLink = ClientPropertyLink(propertyLinkWithClient)

  val validPropertiesView: PropertiesView = PropertiesView(
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
      )
    ),
    parties = Seq(
      APIParty(
        id = 24680,
        authorisedPartyOrganisationId = agentOrgId
      )
    ),
    agents = Some(Nil),
    client = None
  )

  val ownerAuthorisation: OwnerAuthorisation = OwnerAuthorisation(
    authorisationId = 1111,
    status = "APPROVED",
    submissionId = "11111",
    uarn = 11111,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "localAuthRef",
    agents = Nil
  )

  val propertyLinksWithClients: PropertyLinksWithClients = PropertyLinksWithClients(1, 1, 1, 1, Seq())
  val modernisedOwnerAuthResult: ModernisedOwnerAuthResult = ModernisedOwnerAuthResult(1, 1, 1, 1, Seq())
  val ownerAuthResult: OwnerAuthResult = OwnerAuthResult(1, 1, 1, 1, Seq())
  val propertyLinksCount = 5

  val assessments: Assessments = Assessments(
    authorisationId = 1L,
    submissionId = "11111",
    uarn = 111111,
    address = "address",
    pending = false,
    clientOrgName = None,
    capacity = Some("OWNER"),
    assessments = Seq(),
    agents = Seq()
  )

  "create" should {
    "create a new property link submission in modernised" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", today, None)
      val testPropertyLinkSubmission = PropertyLinkRequest(
        uarn = 1,
        organisationId = 1,
        individualId = 1,
        capacityDeclaration = testCapacityDeclaration,
        linkedDate = Instant.now(),
        linkBasis = "TEST_BASIS",
        fileInfo = Seq(FileInfo("filename", "evidenceType")),
        submissionId = "PL12345"
      )

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkingService.create(any())(any(), any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(200)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      status(res) shouldBe ACCEPTED
    }

    "return InternalServerError if property link submission fails" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", today, None)
      val testPropertyLinkSubmission = PropertyLinkRequest(
        uarn = 1,
        organisationId = 1,
        individualId = 1,
        capacityDeclaration = testCapacityDeclaration,
        linkedDate = Instant.now(),
        linkBasis = "TEST_BASIS",
        fileInfo = Seq(FileInfo("filename", "evidenceType")),
        submissionId = "PL12345"
      )

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkingService.create(any())(any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Failed to create PL", 501, 501)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "create on client behalf" should {
    "create a new property link submission in modernised" in {
      val clientId = 100
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", today, None)
      val testPropertyLinkSubmission = PropertyLinkRequest(
        uarn = 1,
        organisationId = 1,
        individualId = 1,
        capacityDeclaration = testCapacityDeclaration,
        linkedDate = Instant.now(),
        linkBasis = "TEST_BASIS",
        fileInfo = Seq(FileInfo("filename", "evidenceType")),
        submissionId = "PL12345"
      )

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkingService.createOnClientBehalf(any(), any())(any(), any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(200)))

      val res = testController.createOnClientBehalf(clientId)(FakeRequest().withBody(plSubmissionJson))
      status(res) shouldBe ACCEPTED
    }

    "return InternalServerError if property link submission fails" in {
      val clientId = 100
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", today, None)
      val testPropertyLinkSubmission = PropertyLinkRequest(
        uarn = 1,
        organisationId = 1,
        individualId = 1,
        capacityDeclaration = testCapacityDeclaration,
        linkedDate = Instant.now(),
        linkBasis = "TEST_BASIS",
        fileInfo = Seq(FileInfo("filename", "evidenceType")),
        submissionId = "PL12345"
      )

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkingService.createOnClientBehalf(any(), any())(any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Failed to create PL", 501, 501)))

      val res = testController.createOnClientBehalf(clientId)(FakeRequest().withBody(plSubmissionJson))
      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getMyPropertyLink" should {
    "return a single my org property link" in {
      when(mockPropertyLinkingService.getMyOrganisationsPropertyLink(any())(any()))
        .thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getMyOrganisationsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(validPropertiesView)
    }

    "return a single my client property link - propertiesView" in {
      when(mockPropertyLinkingService.getClientsPropertyLink(any())(any()))
        .thenReturn(OptionT.some[Future](clientPropertyLink))
      val res = testController.getClientsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(PropertiesView(clientPropertyLink.authorisation, Nil))
    }

    "return a single my client property link - clientPropertyLink" in {
      when(mockPropertyLinkingService.getClientsPropertyLink(any())(any()))
        .thenReturn(OptionT.some[Future](clientPropertyLink))
      val res = testController.getClientsPropertyLink("11111", "clientsPropertyLink")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(clientPropertyLink.authorisation)
    }
  }

  "getMyPropertyLinks" should {
    "return owner property links" in {
      when(mockPropertyLinkingService.getMyOrganisationsPropertyLinks(any(), any())(any()))
        .thenReturn(Future.successful(ownerAuthResult))
      val res =
        testController.getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(), None)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }

    "getMyOrganisationsPropertyLinks" when {

      "return owner property links" in {
        when(mockPropertyLinkingService.getMyOrganisationsPropertyLinks(any(), any())(any()))
          .thenReturn(Future.successful(ownerAuthResult))
        val res = testController.getMyOrganisationsPropertyLinks(
          GetMyOrganisationPropertyLinksParameters(sortField = Some("AGENT")),
          None
        )(FakeRequest())
        status(res) shouldBe OK
      }

    }
    "return client property links" in {
      when(mockPropertyLinkingService.getClientsPropertyLinks(any(), any())(any()))
        .thenReturn(OptionT.some[Future](propertyLinksWithClients))
      val res = testController.getClientsPropertyLinks(GetMyClientsPropertyLinkParameters(), None)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }
  }

  "getAssignedPropertyLinksForClient" should {
    "return client property links" in {
      when(mockPropertyLinkingService.getClientPropertyLinks(any(), any(), any())(any()))
        .thenReturn(OptionT.some[Future](propertyLinksWithClients))
      val res =
        testController.getClientPropertyLinks(111L, GetClientPropertyLinksParameters(), None)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }
  }

  "getMyAgentPropertyLinks" should {
    val agentCode = 1
    "return owner property links by agent code" in {
      when(mockPropertyLinkingService.getMyAgentPropertyLinks(any(), any(), any())(any()))
        .thenReturn(Future.successful(ownerAuthResult))
      val res = testController.getMyAgentPropertyLinks(
        agentCode,
        GetMyOrganisationPropertyLinksParameters(),
        PaginationParams(1, 10, true)
      )(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }
  }

  "getMyAgentAppointablePropertyLinks" should {
    val agentCode = 1
    "return property links by agent code" in {
      when(mockPropertyLinkingService.getMyAgentAvailablePropertyLinks(any(), any(), any())(any()))
        .thenReturn(Future.successful(ownerAuthResult))
      val res = testController.getMyAgentAppointablePropertyLinks(
        agentCode,
        GetMyOrganisationPropertyLinksParameters(),
        Some(PaginationParams(1, 10, true))
      )(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }
  }

  "getMyPropertyLinksCount" should {

    "return owner property links count" in {
      when(mockPropertyLinkingService.getMyOrganisationsPropertyLinksCount()(any()))
        .thenReturn(Future.successful(propertyLinksCount))
      val res = testController.getMyOrganisationsPropertyLinksCount()(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(propertyLinksCount)
    }
  }

  "getMyOrganisationsAssessments" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getMyOrganisationsAssessments(mEq(submissionId))(any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getMyOrganisationsAssessments(submissionId)(FakeRequest())

      status(res) shouldBe OK
    }
  }

  "getClientsAssessments" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getClientsAssessments(mEq(submissionId))(any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getClientsAssessments(submissionId)(FakeRequest())

      status(res) shouldBe OK
    }
  }

  "getMyOrganisationsAgents" should {
    "return agents list for the organisation" in {
      when(mockPropertyLinkingService.getMyOrganisationsAgents()(any()))
        .thenReturn(Future.successful(organisationsAgentsList))

      val res = testController.getMyOrganisationsAgents()(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(organisationsAgentsList)
    }

    "return empty agents list for the organisation if the organisation have no agents" in {
      when(mockPropertyLinkingService.getMyOrganisationsAgents()(any()))
        .thenReturn(Future.successful(emptyOrganisationsAgentsList))
      val res = testController.getMyOrganisationsAgents()(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(emptyOrganisationsAgentsList)
    }
  }

  "getMyClients" should {
    "return clients for the agent organisation" in {
      when(mockPropertyLinkingService.getMyClients(any(), any())(any()))
        .thenReturn(Future.successful(clientsList))

      val res = testController.getMyClients(
        Some(GetClientsParameters()),
        Some(PaginationParams(startPoint = 1, pageSize = 10, requestTotalRowCount = true))
      )(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(clientsList)
    }

    "return empty clients list for the organisation if the agent organisation has no clients" in {
      when(mockPropertyLinkingService.getMyClients(any(), any())(any()))
        .thenReturn(Future.successful(emptyClientsList))

      val res = testController.getMyClients(
        Some(GetClientsParameters()),
        Some(PaginationParams(startPoint = 1, pageSize = 10, requestTotalRowCount = true))
      )(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(emptyClientsList)
    }
  }

  trait ClientPropertySetup {
    protected def propertiesView(authId: Long): PropertiesView =
      PropertiesView(
        authorisationId = authId,
        uarn = 123456,
        authorisationStatus = "OPEN",
        startDate = today,
        endDate = Some(today.plusDays(2)),
        submissionId = "PL12345",
        address = None,
        NDRListValuationHistoryItems = Seq.empty[APIValuationHistory],
        parties = Seq(
          APIParty(
            1L,
            agentOrgId
          )
        ),
        agents = None,
        client = None
      )

    val groupAccount: GroupAccount = GroupAccount(
      id = 100L,
      groupId = "grpId",
      companyName = "ACME",
      addressId = 1234L,
      email = "email@server.com",
      phone = "01234567",
      isAgent = false,
      agentCode = Some(54321L)
    )
  }
}
