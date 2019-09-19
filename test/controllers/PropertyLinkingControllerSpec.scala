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

package controllers

import java.time.Instant

import basespecs.BaseControllerSpec
import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import cats.data._
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.requests.PropertyLinkRequest
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyLinkingControllerSpec extends BaseControllerSpec {

  lazy val testController = new PropertyLinkingController(
    authenticated = preAuthenticatedActionBuilders(),
    authorisationSearchApi = mockAuthorisationSearchApi,
    mdtpDashboardManagementApi = mockMdtpDashboardManagementApi,
    propertyLinkService = mockPropertyLinkingService,
    assessmentService = mockAssessmentService,
    customerManagementApi = mockCustomerManagementApi,
    auditingService = mock[AuditingService],
    agentQueryParameterEnabledExternal = true)

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
        checkPermission = "APPROVED",
        challengePermission = "APPROVED",
        endDate = None)))),
    agents = Some(Nil))


  val ownerAuthorisation = OwnerAuthorisation(
    authorisationId = 1111,
    status = "APPROVED",
    submissionId = "11111",
    uarn = 11111,
    address = "1 HIGH STREET, BRIGHTON",
    localAuthorityRef = "localAuthRef",
    agents = Nil)

  val propertyLinksWithClients = PropertyLinksWithClients(1, 1, 1, 1, Seq())
  val ownerAuthResult = OwnerAuthResult(1, 1, 1, 1, Seq())

  val assessments = Assessments(
    authorisationId = 1L,
    submissionId = "11111",
    uarn = 111111,
    address = "address",
    pending = false,
    capacity = Some("OWNER"),
    assessments = Seq(),
    agents = Seq())

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

      when(mockPropertyLinkingService.create(any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

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
        .thenReturn(Future.failed(Upstream5xxResponse("Failed to create PL", 501, 501)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getMyPropertyLink" should {
    "return a single my org property link" in {
      when(mockPropertyLinkingService.getMyOrganisationsPropertyLink(any())(any(), any()))
        .thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getMyOrganisationsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(validPropertiesView)
    }

    "return a single my client property link" in {
      when(mockPropertyLinkingService.getClientsPropertyLink(any())(any(), any()))
        .thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getClientsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(validPropertiesView)
    }
  }

  "getMyPropertyLinks" should {
    "return owner property links" in {
      when(mockPropertyLinkingService.getMyOrganisationsPropertyLinks(any(), any())(any(), any()))
        .thenReturn(OptionT.some[Future](ownerAuthResult))
      val res = testController.getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(), None, None)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }

    "search via authorisationSearchApi when AGENT sortField" when {

      "organisationId is provided" in {
        when(mockPropertyLinkingService.getMyOrganisationsPropertyLinks(any(), any())(any(), any()))
          .thenReturn(OptionT.some[Future](ownerAuthResult))
        val res = testController.getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(sortField = Some("AGENT")), None, None)(FakeRequest())

        status(res) shouldBe BAD_REQUEST
      }

      "organisationId is NOT provided" in {
        val orgId: Long = 123L
        when(mockAuthorisationSearchApi.searchAndSort(mEq(orgId), any(), mEq(Some("AGENT")), any(), any(), any(), any(), any(), any())(any()))
          .thenReturn(Future.successful(ownerAuthResult))
        val res = testController.getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(sortField = Some("AGENT")), None, Some(orgId))(FakeRequest())

        status(res) shouldBe OK
      }

    }
    "return client property links" in {
      when(mockPropertyLinkingService.getClientsPropertyLinks(any(), any())(any(), any())).thenReturn(OptionT.some[Future](propertyLinksWithClients))
      val res = testController.getClientsPropertyLinks(GetMyClientsPropertyLinkParameters(), None)(FakeRequest())

      status(res) shouldBe OK
      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)
    }
  }

  "getMyOrganisationsAssessments" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getMyOrganisationsAssessments(mEq(submissionId))(any(), any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getMyOrganisationsAssessments(submissionId)(FakeRequest())

      status(res) shouldBe OK
    }
  }

  "getClientsAssessments" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getClientsAssessments(mEq(submissionId))(any(), any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getClientsAssessments(submissionId)(FakeRequest())

      status(res) shouldBe OK
    }
  }


  "getMyOrganisationsAssessmentsWithCapacity" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getMyOrganisationsAssessments(mEq(submissionId))(any(), any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getMyOrganisationsAssessmentsWithCapacity(submissionId, 1L)(FakeRequest())

      status(res) shouldBe OK
    }
  }

  "getClientsAssessmentsWithCapacity" should {
    "return property links with assessments" in {
      val submissionId = "SUB123"
      when(mockAssessmentService.getClientsAssessments(mEq(submissionId))(any(), any()))
        .thenReturn(OptionT.some[Future](assessments))

      val res = testController.getClientsAssessmentsWithCapacity(submissionId, 1L)(FakeRequest())

      status(res) shouldBe OK
    }
  }

  trait ClientPropertySetup {
    protected def propertiesView(authId: Long) = PropertiesView(
      authorisationId = authId,
      uarn = 123456,
      authorisationStatus = "OPEN",
      startDate = today,
      endDate = Some(today.plusDays(2)),
      submissionId = "PL12345",
      address = None,
      NDRListValuationHistoryItems = Seq.empty[APIValuationHistory],
      parties = Seq(APIParty(1L, "APPROVED", 1L, Seq(Permissions(1L, "START", "START", None)))),
      agents = None
    )

    val groupAccount = GroupAccount(
      id = 100L,
      groupId = "grpId",
      companyName = "ACME",
      addressId = 1234L,
      email = "email@server.com",
      phone = "01234567",
      isAgent = false,
      agentCode = 54321L
    )
  }

  "clientProperty" should {
    "return 200 OK with client property" in new ClientPropertySetup {
      val authorisationId: Long = 1234L
      when(mockMdtpDashboardManagementApi.get(mEq(authorisationId))(any()))
        .thenReturn(Future.successful(Some(propertiesView(authorisationId))))
      when(mockCustomerManagementApi.getDetailedGroupAccount(any())(any()))
        .thenReturn(Future.successful(Some(groupAccount)))

      val res = testController.clientProperty(authorisationId, 1L, 1L)(FakeRequest())

      status(res) shouldBe OK
    }


    "return 404 Not Found" when {
      "dashboard management api returns no properties" in {
        val authorisationId: Long = 1234L
        when(mockMdtpDashboardManagementApi.get(mEq(authorisationId))(any()))
          .thenReturn(Future.successful(None))

        val res = testController.clientProperty(authorisationId, 1L, 1L)(FakeRequest())

        status(res) shouldBe NOT_FOUND
      }
    }
  }
}
