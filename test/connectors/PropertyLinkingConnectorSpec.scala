/*
 * Copyright 2017 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models.PaginationParams
import play.api.http.ContentTypes
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class PropertyLinkingConnectorSpec extends ContentTypes
    with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new PropertyLinkingConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl: String = mockServerUrl
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
      await(connector.find(organisationId, PaginationParams(1, 25, requestTotalRowCount = false))(hc)).authorisations.size shouldBe 0
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
      await(connector.searchAndSort(organisationId, PaginationParams(1, 10, false))(hc)).authorisations.size shouldBe 3

      stubFor(get(urlEqualTo(searchUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(clientSearchResultWithoutAgentStatus)
        )
      )
      await(connector.searchAndSort(organisationId, PaginationParams(1, 10, false))(hc)).authorisations.size shouldBe 3
    }
  }

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
