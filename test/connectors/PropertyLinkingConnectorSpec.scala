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

package connectors

import java.time.{LocalDate, ZoneOffset}

import com.github.tomakehurst.wiremock.client.WireMock.{stubFor, _}
import helpers.SimpleWsHttpTestApplication
import models._
import play.api.http.ContentTypes
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class PropertyLinkingConnectorSpec extends ContentTypes
    with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new PropertyLinkingConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl: String = mockServerUrl
  }

  "PropertyLinkingConnector.getAssessment" should {
    "return a properties view for an invalid authorisation Id." in {

      val authorisationId = 123456789
      val listYear = 2017
      val getAssessmentUrl =  s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
        s"?listYear=$listYear" +
        s"&authorisationId=$authorisationId"

      stubFor(get(urlEqualTo(getAssessmentUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(invalidPropertiesViewResult)
        ))

      val result = connector.get(authorisationId)
      result.getOrElse("Passed") shouldBe "Passed"
    }
  }

  "PropertyLinkingConnector.getAssessment" should {
    "return a properties view for a valid authorisation Id." in {

      val authorisationId = 123456789
      val listYear = 2017

      val getAssessmentUrl =  s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
        s"?listYear=$listYear" +
        s"&authorisationId=$authorisationId"

      stubFor(get(urlEqualTo(getAssessmentUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validPropertiesViewResult)
        ))

      val result = connector.get(authorisationId)
      result.getOrElse("Invalid Test: None should not be returned") shouldBe validPropertiesView
    }
  }

  "PropertyLinkingConnector.get" should {
    "not return a properties view for an invalid status." in {

      val authorisationId = 123456789
      val listYear = 2017

      val getUrl =  s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
        s"?listYear=$listYear" +
        s"&authorisationId=$authorisationId"

      stubFor(get(urlEqualTo(getUrl))
          .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(invalidPropertiesViewResult)
      ))

      val result = connector.get(authorisationId)

      result.getOrElse("Passed") shouldBe "Passed"
    }
  }

  "PropertyLinkingConnector.get" should {
    "return a properties view for a valid authorisation id containing valid status." in {

      val authorisationId = 123456789
      val listYear = 2017
      val getUrl =  s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
        s"?listYear=$listYear" +
        s"&authorisationId=$authorisationId"

      stubFor(get(urlEqualTo(getUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validPropertiesViewResult)
        ))

      val result = connector.get(authorisationId)
      result.getOrElse("Invalid Test: None should not be returned") shouldBe validPropertiesView
    }
  }

  "PropertyLinkingConnector.create" should {
    "create a property linking request" in {

      val createUrl = "/property-management-api/property/save_property_link"
      val linkingRequest: APIPropertyLinkRequest = APIPropertyLinkRequest(
        uarn = 1234567890,
        authorisationOwnerOrganisationId = 987654,
        createDatetime = instant,
        authorisationOwnerPersonId = 13579,
        authorisationMethod = "OTHER",
        uploadedFiles = Seq(FileInfo("file1", "council bill")),
        submissionId = "abcde123",
        authorisationOwnerCapacity = "OWNER_OCCUPIER",
        startDate = date,
        endDate = Some(date)
      )

      stubFor(post(urlEqualTo(createUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(emptyResponse)
        )
      )
      val result = await(connector.create(linkingRequest))
      result shouldBe ()
    }
  }

  "PropertyLinkingConnector.find" should {
    "filter properties that are revoked, or declined" in {

      val organisationId = 123
      val propertiesUrl = s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
        s"?listYear=2017" +
        s"&organisationId=$organisationId" +
        s"&startPoint=1" +
        s"&pageSize=25" +
        s"&requestTotalRowCount=false"

      stubFor(get(urlEqualTo(propertiesUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(declinedAndRevokedProperties)
        )
      )
      val result = await(connector.find(organisationId, PaginationParams(1, 25, requestTotalRowCount = false))(hc))
      result.authorisations.size shouldBe 0
      result.authorisations.foreach(_.NDRListValuationHistoryItems.foreach(valuation => valuation.address shouldBe valuation.address.toUpperCase))
    }
  }

  "PropertyLinkingConnector.searchAndSort" should {
    "not care if agent status is present or not" in {
      implicit val hc = HeaderCarrier()
      val http = fakeApplication.injector.instanceOf[WSHttp]

      val connector = new PropertyLinkingConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
        override lazy val baseUrl: String = mockServerUrl
      }

      val organisationId = 123L
      val searchUrl = s"/authorisation-search-api/owners/$organisationId/authorisations?start=1&size=10"

      stubFor(get(urlEqualTo(searchUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(clientSearchResultWithAgentStatus)
        )
      )
      val resultWithAgent = await(connector.searchAndSort(organisationId, PaginationParams(1, 10, false))(hc))
      resultWithAgent.authorisations.size shouldBe 4
      resultWithAgent.authorisations.foreach(owner => owner.address shouldBe owner.address.toUpperCase)

      stubFor(get(urlEqualTo(searchUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(clientSearchResultWithoutAgentStatus)
        )
      )
      val result = await(connector.searchAndSort(organisationId, PaginationParams(1, 10, false))(hc))
      result.authorisations.size shouldBe 3
      result.authorisations.foreach(owner => owner.address shouldBe owner.address.toUpperCase)
    }
  }

  val date = LocalDate.parse("2018-09-05")
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)

  val validPropertiesView = PropertiesView(
    authorisationId = 987654,
    uarn = 1234567890,
    authorisationOwnerOrganisationId = 987654,
    authorisationOwnerPersonId = 13579,
    authorisationStatus = "ALLOW",
    authorisationMethod = "OTHER",
    authorisationOwnerCapacity = "OWNER_OCCUPIER",
    createDatetime = instant,
    startDate = date,
    endDate = Some(date),
    submissionId = "abc123",
    NDRListValuationHistoryItems = Seq(APIValuationHistory(
      asstRef = 125689,
      listYear = "2017",
      uarn = 923411,
      effectiveDate = date,
      rateableValue = Some(2599),
      address = "1 HIGH STREET, BRIGHTON",
      billingAuthorityReference = "VOA1"
    )),
    parties = Seq(APIParty(
      id = 24680,
      authorisedPartyStatus = "APPROVED",
      authorisedPartyOrganisationId = 123456,
      permissions = Seq(Permissions(
        id = 13579,
        checkPermission = "APPROVED",
        challengePermission = "APPROVED",
        endDate = None
      )
      )
    ))
  )

  lazy val emptyResponse = "{}"

  lazy val invalidPropertiesViewResult = validPropertiesViewResult.replace("ALLOW", "DECLINED")

  lazy val validPropertiesViewResult =
    s"""{
       |  "authorisationId": 987654,
       |	"uarn": 1234567890,
       |	"authorisationOwnerOrganisationId": 987654,
       |	"authorisationOwnerPersonId": 13579,
       |	"authorisationStatus": "ALLOW",
       |	"authorisationMethod": "OTHER",
       |	"authorisationOwnerCapacity": "OWNER_OCCUPIER",
       |	"createDatetime": "2018-09-05T00:00:00.000+0000",
       |	"startDate": "2018-09-05",
       |	"endDate": "2018-09-05",
       |	"submissionId": "abc123",
       |	"NDRListValuationHistoryItems": [{
       |		"asstRef": 125689,
       |		"listYear": "2017",
       |		"uarn": 923411,
       |		"effectiveDate": "2018-09-05",
       |		"rateableValue": 2599,
       |		"address": "1 High Street, Brighton",
       |		"billingAuthorityReference": "VOA1"
       |	}],
       |	"parties": [{
       |		"id": 24680,
       |		"authorisedPartyStatus": "APPROVED",
       |		"authorisedPartyOrganisationId": 123456,
       |		"permissions": [{
       |			"id": 13579,
       |			"checkPermission": "APPROVED",
       |			"challengePermission": "APPROVED"
       |		}]
       |	}]
       }""".stripMargin

  lazy val validPropertiesViewResultX =
    s"""{
      |"authorisationId": 987654,
      |"uarn": 1234567890,
      |"authorisationOwnerOrganisationId": 987654,
      |"authorisationOwnerPersonId": 13579,
      |"authorisationStatus": "ALLOW",
      |"authorisationMethod": "OTHER",
      |"authorisationOwnerCapacity": "OWNER_OCCUPIER",
      |"createDatetime": "2018-09-05T00:00:00.000+0000",
      |"startDate": "2018-09-05",
      |"endDate": "2018-09-05",
      |"submissionId": "abc123",
      |"NDRListValuationHistoryItems": [
      | {
      |   "asstRef": 125689,
      |   "listYear": "2017",
      |   "uarn": 923411,
      |   "effectiveDate": "2018-09-05",
      |   "rateableValue": 2599,
      |   "address": "1 High Street, Brighton",
      |   "billingAuthorityReference": "VOA2"
      | }
      |],
      |"parties": [
      | {
      |   "id": 24680,
      |   "authorisedPartyStatus": "AGENT",
      |   "authorisedPartyOrganisationId": 123456,
      |   "permissions": [
      |   {
      |     "id": 13579,
      |     "checkPermission": "ALLOWED",
      |     "challengePermission": "ALLOWED",
      |     "endDate": "2018-09-02"
      |   }
      |  ]
      | }
      |]
    }""".stripMargin

  lazy val clientSearchResultWithAgentStatus =
    """{
      |  "start": 1,
      |  "size": 15,
      |  "filterTotal": 3,
      |  "total": 10,
      |  "authorisations": [
      |    {
      |      "authorisationId": 10000000000005,
      |      "status": "PENDING",
      |      "submissionId": "a0000000000000000000000000000005",
      |      "uarn": 8735379000,
      |      "address": "1 CLEMENTS ROAD, LONDON, SE16 4DG",
      |      "localAuthorityRef": "1940002152213J"
      |    },
      |    {
      |      "authorisationId": 10000000000005,
      |      "status": "PENDING",
      |      "submissionId": "a0000000000000000000000000000005",
      |      "uarn": 8735379000,
      |      "address": "1 clements road, london, se16 4dg",
      |      "localAuthorityRef": "1940002152213J"
      |    },
      |    {
      |      "authorisationId": 10000000000004,
      |      "status": "REVOKED",
      |      "submissionId": "a0000000000000000000000000000004",
      |      "uarn": 1592189000,
      |      "address": "1 CHORLEY OLD ROAD, BOLTON, BL1 6AB",
      |      "localAuthorityRef": "1S2643050002",
      |      "agents": [
      |        {
      |          "authorisedPartyId": 1000000005,
      |          "organisationId": 1000000005,
      |          "organisationName": "Automated Stub Agent 1",
      |          "status": "REVOKED"
      |        },
      |        {
      |          "authorisedPartyId": 5000000006,
      |          "organisationId": 5000000006,
      |          "organisationName": "Automated Stub Agent 2",
      |          "status": "APPROVED"
      |        }
      |      ]
      |    },
      |    {
      |      "authorisationId": 10000000000003,
      |      "status": "DECLINED",
      |      "submissionId": "a0000000000000000000000000000003",
      |      "uarn": 8444236000,
      |      "address": "1 WESTEND TERRACE, EBBW VALE, NP23 6HS",
      |      "localAuthorityRef": "138030130380000204",
      |      "agents": [
      |        {
      |          "authorisedPartyId": 1000000005,
      |          "organisationId": 1000000005,
      |          "organisationName": "Automated Stub Agent 1",
      |          "status": "APPROVED"
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  lazy val clientSearchResultWithoutAgentStatus =
    """{
      |  "start": 1,
      |  "size": 15,
      |  "filterTotal": 3,
      |  "total": 10,
      |  "authorisations": [
      |    {
      |      "authorisationId": 10000000000005,
      |      "status": "PENDING",
      |      "submissionId": "a0000000000000000000000000000005",
      |      "uarn": 8735379000,
      |      "address": "1 CLEMENTS ROAD, LONDON, SE16 4DG",
      |      "localAuthorityRef": "1940002152213J"
      |    },
      |    {
      |      "authorisationId": 10000000000004,
      |      "status": "REVOKED",
      |      "submissionId": "a0000000000000000000000000000004",
      |      "uarn": 1592189000,
      |      "address": "1 CHORLEY OLD ROAD, BOLTON, BL1 6AB",
      |      "localAuthorityRef": "1S2643050002",
      |      "agents": [
      |        {
      |          "authorisedPartyId": 1000000005,
      |          "organisationId": 1000000005,
      |          "organisationName": "Automated Stub Agent 1"
      |        },
      |        {
      |          "authorisedPartyId": 5000000006,
      |          "organisationId": 5000000006,
      |          "organisationName": "Automated Stub Agent 2"
      |        }
      |      ]
      |    },
      |    {
      |      "authorisationId": 10000000000003,
      |      "status": "DECLINED",
      |      "submissionId": "a0000000000000000000000000000003",
      |      "uarn": 8444236000,
      |      "address": "1 WESTEND TERRACE, EBBW VALE, NP23 6HS",
      |      "localAuthorityRef": "138030130380000204",
      |      "agents": [
      |        {
      |          "authorisedPartyId": 1000000005,
      |          "organisationId": 1000000005,
      |          "organisationName": "Automated Stub Agent 1"
      |        }
      |      ]
      |    }
      |  ]
      |}""".stripMargin

  lazy val declinedAndRevokedProperties =
    """
      |{
      |  "authorisations": [
      |    {
      |      "NDRListValuationHistoryItems": [
      |        {
      |          "address": "4, HERON ROAD IND UNITS, EXETER, EX2 7LL",
      |          "asstRef": 6505006000,
      |          "billingAuthorityReference": "70305000400",
      |          "compositeProperty": "N",
      |          "costPerM2": 65,
      |          "deletedIndicator": false,
      |          "description": "WAREHOUSE AND PREMISES",
      |          "effectiveDate": "2005-03-31",
      |          "listYear": "2005",
      |          "numberOfPreviousProposals": 1,
      |          "origCasenoSeq": 6731412182,
      |          "rateableValue": 16500,
      |          "specialCategoryCode": "096G",
      |          "totalAreaM2": 252.99,
      |          "uarn": 146440182,
      |          "valuationDetailsAvailable": true
      |        }
      |      ],
      |      "authorisationMethod": "OTHER",
      |      "authorisationNotes": [
      |        {
      |          "content": "TestAuthorisationNote",
      |          "createDatetime": "2016-12-17T10:08:20.000+0000",
      |          "createdBy": "TestUser",
      |          "id": 3
      |        }
      |      ],
      |      "authorisationOwnerCapacity": "OWNER_OCCUPIER",
      |      "authorisationOwnerOrganisationId": 2,
      |      "authorisationOwnerPersonId": 2,
      |      "authorisationStatus": "REVOKED",
      |      "createDatetime": "2016-07-01T12:53:51.000+0000",
      |      "endDate": "2016-11-12",
      |      "authorisationId": 112,
      |      "parties": [
      |      ],
      |      "reasonForDecision": "enim nisi sit",
      |      "ruleResults": [],
      |      "selfCertificationDeclarationFlag": true,
      |      "startDate": "2016-05-07",
      |      "submissionId": "ABC99003",
      |      "uarn": 146440182,
      |      "uploadedFiles": [
      |        {
      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
      |          "name": "test.pdf"
      |        },
      |        {
      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
      |          "name": "test.docx"
      |        }
      |      ]
      |    },
      |    {
      |      "NDRListValuationHistoryItems": [
      |        {
      |          "address": "22, HERON ROAD IND UNITS, EXETER, EX2 7LL",
      |          "asstRef": 6505006000,
      |          "billingAuthorityReference": "70305000400",
      |          "compositeProperty": "N",
      |          "costPerM2": 65,
      |          "deletedIndicator": false,
      |          "description": "WAREHOUSE AND PREMISES",
      |          "effectiveDate": "2005-03-31",
      |          "listYear": "2005",
      |          "numberOfPreviousProposals": 1,
      |          "origCasenoSeq": 6731412182,
      |          "rateableValue": 16500,
      |          "specialCategoryCode": "096G",
      |          "totalAreaM2": 252.99,
      |          "uarn": 146440182,
      |          "valuationDetailsAvailable": true
      |        }
      |      ],
      |      "authorisationMethod": "OTHER",
      |      "authorisationNotes": [
      |        {
      |          "content": "TestAuthorisationNote",
      |          "createDatetime": "2016-12-17T10:08:20.000+0000",
      |          "createdBy": "TestUser",
      |          "id": 3
      |        }
      |      ],
      |      "authorisationOwnerCapacity": "OWNER_OCCUPIER",
      |      "authorisationOwnerOrganisationId": 2,
      |      "authorisationOwnerPersonId": 2,
      |      "authorisationStatus": "DECLINED",
      |      "createDatetime": "2016-07-01T12:53:51.000+0000",
      |      "endDate": "2016-11-12",
      |      "authorisationId": 112,
      |      "parties": [
      |      ],
      |      "reasonForDecision": "enim nisi sit",
      |      "ruleResults": [],
      |      "selfCertificationDeclarationFlag": true,
      |      "startDate": "2016-05-07",
      |      "submissionId": "ABC99003",
      |      "uarn": 146440182,
      |      "uploadedFiles": [
      |        {
      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
      |          "name": "test.pdf"
      |        },
      |        {
      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
      |          "name": "test.docx"
      |        }
      |      ]
      |    }
      |  ]
      |}
    """.stripMargin
}
