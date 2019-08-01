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
import binders.GetPropertyLinksParameters
import cats.data._
import cats.implicits._
import connectors._
import connectors.auth._
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.requests.{APIPropertyLinkRequest, PropertyLinkRequest}
import models.searchApi.{AgentAuthResult, OwnerAuthResult, OwnerAuthorisation}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import services.AssessmentService
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PropertyLinkingService
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val request = FakeRequest()
  implicit val modernisedEnrichedRequest = ModernisedEnrichedRequest(FakeRequest(), "XXXXX", "YYYYY")
  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  implicit val fakeHc = HeaderCarrier()


  val mockWS = mock[WSHttp]
  val mockConf = mock[ServicesConfig]
  val mockAddressConnector = mock[AddressConnector]
  val baseUrl = "http://localhost:9999"

  lazy val mockAuthConnector = {
    val m = mock[DefaultAuthConnector]
    when(m.authorise[~[Option[String], Option[String]]](any(), any())(any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(
      new ~(Some("externalId"), Some("groupIdentifier")))
    m
  }

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("run.mode" -> "Test")
    .overrides(bind[WSHttp].qualifiedWith("VoaBackendWsHttp").toInstance(mockWS))
    .overrides(bind[ServicesConfig].toInstance(mockConf))
    .overrides(bind[DefaultAuthConnector].toInstance(mockAuthConnector))
    .build()


  lazy val mockPropertyLinkConnector = mock[PropertyLinkingConnector]

  lazy val mockPropertyLinkService = mock[PropertyLinkingService]

  lazy val mockGroupAccountConnector = mock[GroupAccountConnector]

  lazy val mockPropertyRepresentationConnector = mock[PropertyRepresentationConnector]

  lazy val mockAssessmentService = mock[AssessmentService]

  lazy val mockBrAuth = mock[BusinessRatesAuthConnector]

  lazy val testController = new PropertyLinkingController(mockAuthConnector, mockPropertyLinkService, mockAssessmentService, mockGroupAccountConnector, mock[AuditingService], mockPropertyRepresentationConnector)

  val date = LocalDate.parse("2018-09-05")

  val validPropertiesView = PropertiesView(
    authorisationId = 11111,
    uarn = 33333,
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
        endDate = None)))))


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

      status(res) shouldBe CREATED

    }

    "return InternalServerError if property link submission fails" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", LocalDate.now(), None)
      val testPropertyLinkSubmission = PropertyLinkRequest(1, 1, 1, testCapacityDeclaration, Instant.now(), "TEST_BASIS", Seq(FileInfo("filename", "evidenceType")), "PL12345")
      val testAPIPropertyLinkRequest = APIPropertyLinkRequest.fromPropertyLinkRequest(testPropertyLinkSubmission)

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkService.create(any())(any(), any())).thenReturn(Future.failed(new Upstream5xxResponse("Failed to create PL", 501, 501)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      await(res)

      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getMyPropertyLink" should {
    "return a single my org proprty link" in {

      when(mockPropertyLinkService.getMyOrganisationsPropertyLink(any())(any(), any())).thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getMyOrganisationsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK

      contentAsJson(res) shouldBe Json.toJson(validPropertiesView)

    }

    "return a single my client proprty link" in {

      when(mockPropertyLinkService.getClientsPropertyLink(any())(any(), any())).thenReturn(OptionT.some[Future](validPropertiesView))
      val res = testController.getClientsPropertyLink("11111")(FakeRequest())

      status(res) shouldBe OK

      contentAsJson(res) shouldBe Json.toJson(validPropertiesView)

    }
  }


  "getMyPropertyLinks" should {
    "return owner proprty links" in {

      when(mockPropertyLinkService.getMyOrganisationsPropertyLinks(any(), any())(any(), any())).thenReturn(OptionT.some[Future](ownerAuthResult))

      val res = testController.getMyOrganisationsPropertyLinks(GetPropertyLinksParameters(), None)(FakeRequest())

      status(res) shouldBe OK

      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)

    }


    "return client proprty links" in {

      when(mockPropertyLinkService.getClientsPropertyLinks(any(), any())(any(), any())).thenReturn(OptionT.some[Future](propertyLinksWithClients))
      val res = testController.getClientsPropertyLinks(GetPropertyLinksParameters(), None)(FakeRequest())

      status(res) shouldBe OK

      contentAsJson(res) shouldBe Json.toJson(ownerAuthResult)

    }
  }
}
