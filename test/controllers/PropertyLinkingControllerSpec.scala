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

import java.time.{Instant, LocalDate}

import auditing.AuditingService
import basespecs.BaseControllerSpec
import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import cats.data._
import cats.implicits._
import connectors._
import connectors.authorisationsearch.PropertyLinkingConnector
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.requests.{APIPropertyLinkRequest, PropertyLinkRequest}
import models.searchApi.{OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.{AssessmentService, PropertyLinkingService}
import uk.gov.hmrc.http.{HttpResponse, Upstream5xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyLinkingControllerSpec extends BaseControllerSpec {

  val mockAddressConnector = mock[AddressConnector]

  lazy val mockPropertyLinkConnector = mock[PropertyLinkingConnector]

  lazy val mockPropertyLinkService = mock[PropertyLinkingService]

  lazy val mockPropertyLinkingConnector = mock[PropertyLinkingConnector]

  lazy val mockGroupAccountConnector = mock[GroupAccountConnector]

  lazy val mockPropertyRepresentationConnector = mock[PropertyRepresentationConnector]

  lazy val mockAssessmentService = mock[AssessmentService]

  lazy val mockBrAuth = mock[BusinessRatesAuthConnector]

  lazy val testController = new PropertyLinkingController(preAuthenticatedActionBuilders(), mockPropertyLinkingConnector,  mockPropertyLinkService, mockAssessmentService, mockGroupAccountConnector, mock[AuditingService], mockPropertyRepresentationConnector, true)

  val date = LocalDate.parse("2018-09-05")

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

  val propertyLinksWithClients = PropertyLinksWithClients(1,1,1,1, Seq())
  val ownerAuthResult = OwnerAuthResult(1,1,1,1, Seq())

  val assessments = Assessments(
    1L,
    "11111",
    111111,
    "address",
    false,
    Some("OWNER"),
    Seq(),

    Seq())

  "create" should {
    "create a new property link submission in modernised" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", LocalDate.now(), None)
      val testPropertyLinkSubmission = PropertyLinkRequest(1, 1, 1, testCapacityDeclaration, Instant.now(), "TEST_BASIS", Seq(FileInfo("filename", "evidenceType")), "PL12345")
      val testAPIPropertyLinkRequest = APIPropertyLinkRequest.fromPropertyLinkRequest(testPropertyLinkSubmission)

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkService.create(any())(any(), any())).thenReturn(Future.successful(HttpResponse(200)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))

      await(res)

      status(res) mustBe ACCEPTED

    }

    "return InternalServerError if property link submission fails" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", LocalDate.now(), None)
      val testPropertyLinkSubmission = PropertyLinkRequest(1, 1, 1, testCapacityDeclaration, Instant.now(), "TEST_BASIS", Seq(FileInfo("filename", "evidenceType")), "PL12345")
      val testAPIPropertyLinkRequest = APIPropertyLinkRequest.fromPropertyLinkRequest(testPropertyLinkSubmission)

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkService.create(any())(any(), any())).thenReturn(Future.failed(new Upstream5xxResponse("Failed to create PL", 501, 501)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      await(res)

      status(res) mustBe INTERNAL_SERVER_ERROR
    }
  }

  "getMyPropertyLink" should {
    "return a single my org property link" in {

      when(mockPropertyLinkService.getMyOrganisationsPropertyLink(any())(any(), any())).thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getMyOrganisationsPropertyLink("11111")(FakeRequest())

      status(res) mustBe OK

      contentAsJson(res) mustBe Json.toJson(validPropertiesView)

    }

    "return a single my client property link" in {

      when(mockPropertyLinkService.getClientsPropertyLink(any())(any(), any())).thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getClientsPropertyLink("11111")(FakeRequest())

      status(res) mustBe OK

      contentAsJson(res) mustBe Json.toJson(validPropertiesView)

    }
  }


  "getMyPropertyLinks" should {
    "return owner property links" in {

      when(mockPropertyLinkService.getMyOrganisationsPropertyLinks(any(), any())(any(), any())).thenReturn(OptionT.some[Future](ownerAuthResult))

      val res = testController.getMyOrganisationsPropertyLinks(GetMyOrganisationPropertyLinksParameters(), None, None)(FakeRequest())

      status(res) mustBe OK

      contentAsJson(res) mustBe Json.toJson(ownerAuthResult)

    }


    "return client property links" in {

      when(mockPropertyLinkService.getClientsPropertyLinks(any(), any())(any(), any())).thenReturn(OptionT.some[Future](propertyLinksWithClients))
      val res = testController.getClientsPropertyLinks(GetMyClientsPropertyLinkParameters(), None)(FakeRequest())

      status(res) mustBe OK

      contentAsJson(res) mustBe Json.toJson(ownerAuthResult)

    }
  }
}
