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
import helpers.WithSimpleWsHttpTestApplication
import models.PaginationParams
import play.api.http.ContentTypes
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.ExecutionContext.Implicits.global

class PropertyLinkingConnectorSpec
  extends ContentTypes
    with WireMockSpec with WithSimpleWsHttpTestApplication {

  "PropertyLinkingConnector.find" should {
    "filter properties that are revoked, or declined" in {
      implicit val hc = HeaderCarrier()
      val http = fakeApplication.injector.instanceOf[WSHttp]

      val connector = new PropertyLinkingConnector(http) {
        override lazy val baseUrl: String = mockServerUrl
      }

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
