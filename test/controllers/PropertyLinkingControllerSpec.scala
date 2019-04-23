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
import connectors._
import connectors.auth._
import models.searchApi.AgentAuthResultBE
import models.{APIPropertyLinkRequest, CapacityDeclaration, _}
import org.mockito.ArgumentMatchers.{eq => mockEq, _}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
//import org.scalatest.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.PropertyLinkingService
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

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

  "given authorised access, find" should {

    when(mockConf.baseUrl(any())).thenReturn(baseUrl)

    "only return the user's properties" in {
      reset(mockWS)
      val testPropertyLinkingController: PropertyLinkingController = fakeApplication.injector.instanceOf[PropertyLinkingController]
      val userOrgId = 111

      val dummyProperties = Seq(
        PropertiesView(101, 101, userOrgId, 103, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil, Nil),
        PropertiesView(102, 102, userOrgId, 104, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil, Nil))

      val propertiesUrl = s"$baseUrl/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
        s"?listYear=2017" +
        s"&organisationId=$userOrgId" +
        s"&startPoint=1" +
        s"&pageSize=25" +
        s"&requestTotalRowCount=false"
      val ec = scala.concurrent.ExecutionContext.Implicits.global

      when(mockWS.GET(mockEq(propertiesUrl))(any(classOf[HttpReads[PropertiesViewResponse]]), any(), any())).thenReturn(Future(PropertiesViewResponse(None, dummyProperties)))

      val repUrl = s"$baseUrl/authorisation-search-api/agents/$userOrgId/authorisations?start=1&size=15&representationStatus=PENDING"
      when(mockWS.GET(mockEq(repUrl))(any(classOf[HttpReads[AgentAuthResultBE]]), any(), any())).thenReturn(AgentAuthResultBE(15, 1, 0, 0, Nil))

      val res = testPropertyLinkingController.find(userOrgId, PaginationParams(1, 25, requestTotalRowCount = false))(FakeRequest().withHeaders(AUTHORIZATION -> """Bearer 123456789"""))
      status(res) shouldBe OK
      val uarns = Json.parse(contentAsString(res)).as[PropertyLinkResponse].propertyLinks.map(_.uarn)
      uarns shouldBe Seq(101, 102)
    }

    "only call modernized once per linked party" in {
      reset(mockWS)
      val testPropertyLinkingController: PropertyLinkingController = fakeApplication.injector.instanceOf[PropertyLinkingController]
      val userOrgId = 111

      //Authorised Party ID is unique for each relationship, even if it's the same agent
      def baseParty1() = APIParty(Random.nextInt, "APPROVED", 1001, Seq(Permissions(10001, "", "", None)))

      def baseParty2() = baseParty1().copy(id = Random.nextInt, authorisedPartyOrganisationId = 1002)

      val dummyProperties = Seq(
        PropertiesView(101, 101, userOrgId, 103, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())),
        PropertiesView(102, 102, userOrgId, 104, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())),
        PropertiesView(103, 103, userOrgId, 105, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())),
        PropertiesView(104, 104, userOrgId, 106, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())),
        PropertiesView(105, 105, userOrgId, 107, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())),
        PropertiesView(106, 106, userOrgId, 108, "AAA", "ASDf", "string", Instant.now(), LocalDate.now(), None, "1231", Nil,
          Seq(baseParty1(), baseParty2())))

      val propertiesUrl = s"$baseUrl/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
        s"?listYear=2017" +
        s"&organisationId=$userOrgId" +
        s"&startPoint=1" +
        s"&pageSize=25" +
        s"&requestTotalRowCount=false".toString
      when(mockWS.GET(mockEq(propertiesUrl))(any(classOf[HttpReads[PropertiesViewResponse]]), any(), any())).thenReturn(Future(PropertiesViewResponse(None, dummyProperties)))

      val repUrl = s"$baseUrl/authorisation-search-api/agents/$userOrgId/authorisations?start=1&size=15&representationStatus=PENDING"
      when(mockWS.GET(mockEq(repUrl))(any(classOf[HttpReads[AgentAuthResultBE]]), any(), any())).thenReturn(AgentAuthResultBE(1, 15, 0, 0, Nil))

      val detailedGroup1 = APIDetailedGroupAccount(1001, "ggGroup11", 1111, GroupDetails(1, true, "orgName", "email@add.res", None), Nil)
      val detailedGroup2 = APIDetailedGroupAccount(1002, "ggGroup22", 2222, GroupDetails(1, true, "orgName", "email@add.res", None), Nil)

      val org1001Url = s"$baseUrl/customer-management-api/organisation?organisationId=1001"
      val org1002Url = s"$baseUrl/customer-management-api/organisation?organisationId=1002"
      when(mockWS.GET(mockEq(org1001Url))(any(classOf[HttpReads[Option[APIDetailedGroupAccount]]]), any(), any())).thenReturn(Some(detailedGroup1))
      when(mockWS.GET(mockEq(org1002Url))(any(classOf[HttpReads[Option[APIDetailedGroupAccount]]]), any(), any())).thenReturn(Some(detailedGroup2))

      val res = testPropertyLinkingController.find(userOrgId, PaginationParams(1, 25, requestTotalRowCount = false))(FakeRequest())
      status(res) shouldBe OK
      val uarns = Json.parse(contentAsString(res)).as[PropertyLinkResponse].propertyLinks.map(_.uarn)
      uarns shouldBe Seq(101, 102, 103, 104, 105, 106)
      verify(mockWS, times(1)).GET(mockEq(propertiesUrl))(any(classOf[HttpReads[PropertiesViewResponse]]), any(), any())
      verify(mockWS, times(1)).GET(mockEq(org1001Url))(any(classOf[HttpReads[Option[APIDetailedGroupAccount]]]), any(), any())
      verify(mockWS, times(1)).GET(mockEq(org1002Url))(any(classOf[HttpReads[Option[APIDetailedGroupAccount]]]), any(), any())
    }
  }

  "create" should {
    "create a new property link submission in modernised" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", LocalDate.now(), None)
      val testPropertyLinkSubmission = PropertyLinkRequest(1, 1, 1, testCapacityDeclaration, Instant.now(), "TEST_BASIS", Seq(FileInfo("filename", "evidenceType")), "PL12345")
      val testAPIPropertyLinkRequest = APIPropertyLinkRequest.fromPropertyLinkRequest(testPropertyLinkSubmission)

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkService.create(testAPIPropertyLinkRequest)(fakeHc, modernisedEnrichedRequest)).thenReturn(Future.successful(HttpResponse(200)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      await(res)

      status(res) shouldBe CREATED

      //val testPropertyLinkingController: PropertyLinkingController = fakeApplication.injector.instanceOf[PropertyLinkingController]


    }

    "return InternalServerError if property link submission fails" in {
      val testCapacityDeclaration = CapacityDeclaration("TEST_CAPACITY", LocalDate.now(), None)
      val testPropertyLinkSubmission = PropertyLinkRequest(1, 1, 1, testCapacityDeclaration, Instant.now(), "TEST_BASIS", Seq(FileInfo("filename", "evidenceType")), "PL12345")
      val testAPIPropertyLinkRequest = APIPropertyLinkRequest.fromPropertyLinkRequest(testPropertyLinkSubmission)

      val plSubmissionJson = Json.toJson(testPropertyLinkSubmission)

      when(mockPropertyLinkService.create(testAPIPropertyLinkRequest)).thenReturn(Future.failed(new Upstream5xxResponse("Failed to create PL", 501, 501)))

      val res = testController.create()(FakeRequest().withBody(plSubmissionJson))
      await(res)

      status(res) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "getMyPropertyLink" should {
    "return a single my org proprty link" in {
      //val res = testController.getMyPropertyLink(11111)()
    }
  }

  lazy val mockPropertyLinkConnector = mock[PropertyLinkingConnector]

  lazy val mockPropertyLinkService = mock[PropertyLinkingService]

  lazy val mockService = mock[PropertyLinkingService]

  lazy val mockGroupAccountConnector = mock[GroupAccountConnector]

  lazy val mockPropertyRepresentationConnector = mock[PropertyRepresentationConnector]

  lazy val mockBrAuth = mock[BusinessRatesAuthConnector]

  lazy val testController = new PropertyLinkingController(mockAuthConnector, mockPropertyLinkConnector, mockService, mockGroupAccountConnector, mock[AuditingService], mockPropertyRepresentationConnector)

}
