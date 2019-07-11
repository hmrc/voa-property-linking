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

///*
// * Copyright 2019 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package models
//
//import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
//
//import helpers.SimpleWsHttpTestApplication
//import play.api.libs.json.Json
//import uk.gov.hmrc.play.test.UnitSpec
//
//class PropertiesViewSpec extends UnitSpec with SimpleWsHttpTestApplication {
//
//  "APIAuthorisation" should {
//    "be deserialised property" in {
//      val authorisation = Json.parse(DummyData.testData).as[PropertiesView]
//      authorisation shouldBe PropertiesView(112, 146440182, "MORE_EVIDENCE_REQUIRED",
//        LocalDate.of(2016, 5, 7),
//        Some(LocalDate.of(2016, 11, 12)),
//        "Address",
//        "ABC99003", Seq(
//          APIValuationHistory(
//            6505006000L,
//            "2005",
//            146440182,
//            LocalDate.of(2005, 3, 1),
//            Some(16500),
//            "4, HERON ROAD IND UNITS, EXETER, EX2 7LL", "70305000400"),
//          APIValuationHistory(
//            14345902000L,
//            "2010",
//            146440182,
//            LocalDate.of(2010, 3, 1),
//            Some(17750),
//            "4, HERON ROAD IND UNITS, EXETER, EX2 7LL", "70305000400"),
//          APIValuationHistory(
//            10176424000L,
//            "2010",
//            146440182,
//            LocalDate.of(2010, 3, 1),
//            Some(20000),
//            "4, HERON ROAD IND UNITS, EXETER, EX2 7LL", "70305000400")),
//        Nil,
//        Nil
//      )
//    }
//  }
//}
//
//object DummyData {
//
//  val testData =
//    """
//      |    {
//      |      "NDRListValuationHistoryItems": [
//      |        {
//      |          "address": "4, HERON ROAD IND UNITS, EXETER, EX2 7LL",
//      |          "asstRef": 6505006000,
//      |          "billingAuthorityReference": "70305000400",
//      |          "compositeProperty": "N",
//      |          "costPerM2": 65,
//      |          "deletedIndicator": false,
//      |          "description": "WAREHOUSE AND PREMISES",
//      |          "effectiveDate": "2005-03-01",
//      |          "listYear": "2005",
//      |          "numberOfPreviousProposals": 1,
//      |          "origCasenoSeq": 6731412182,
//      |          "rateableValue": 16500,
//      |          "specialCategoryCode": "096G",
//      |          "totalAreaM2": 252.99,
//      |          "uarn": 146440182,
//      |          "valuationDetailsAvailable": true
//      |        },
//      |        {
//      |          "address": "4, HERON ROAD IND UNITS, EXETER, EX2 7LL",
//      |          "asstRef": 14345902000,
//      |          "billingAuthorityReference": "70305000400",
//      |          "compositeProperty": "N",
//      |          "costPerM2": 66.5,
//      |          "deletedIndicator": false,
//      |          "description": "WAREHOUSE AND PREMISES",
//      |          "effectiveDate": "2010-03-01",
//      |          "listAlterationDate": "2012-12-17",
//      |          "listYear": "2010",
//      |          "numberOfPreviousProposals": 1,
//      |          "origCasenoSeq": 19314744537,
//      |          "rateableValue": 17750,
//      |          "settlementCode": "A",
//      |          "specialCategoryCode": "096G",
//      |          "totalAreaM2": 269.04,
//      |          "transitionalCertificate": false,
//      |          "uarn": 146440182,
//      |          "valuationDetailsAvailable": true
//      |        },
//      |        {
//      |          "address": "4, HERON ROAD IND UNITS, EXETER, EX2 7LL",
//      |          "asstRef": 10176424000,
//      |          "billingAuthorityReference": "70305000400",
//      |          "compositeProperty": "N",
//      |          "deletedIndicator": false,
//      |          "description": "WAREHOUSE AND PREMISES",
//      |          "effectiveDate": "2010-03-01",
//      |          "listYear": "2010",
//      |          "numberOfPreviousProposals": 1,
//      |          "origCasenoSeq": 12312194182,
//      |          "rateableValue": 20000,
//      |          "specialCategoryCode": "1",
//      |          "transitionalCertificate": false,
//      |          "uarn": 146440182,
//      |          "valuationDetailsAvailable": true
//      |        }
//      |      ],
//      |      "authorisationMethod": "OTHER",
//      |      "authorisationNotes": [
//      |        {
//      |          "content": "TestAuthorisationNote",
//      |          "createDatetime": "2016-12-17T10:08:20.000+0000",
//      |          "createdBy": "TestUser",
//      |          "id": 3
//      |        }
//      |      ],
//      |      "authorisationOwnerCapacity": "OWNER_OCCUPIER",
//      |      "authorisationOwnerOrganisationId": 2,
//      |      "authorisationOwnerPersonId": 3,
//      |      "authorisationStatus": "MORE_EVIDENCE_REQUIRED",
//      |      "createDatetime": "2016-03-01T12:53:51.000+0000",
//      |      "endDate": "2016-11-12",
//      |      "authorisationId": 112,
//      |      "parties": [
//      |      ],
//      |      "reasonForDecision": "enim nisi sit",
//      |      "ruleResults": [],
//      |      "selfCertificationDeclarationFlag": true,
//      |      "startDate": "2016-05-07",
//      |      "submissionId": "ABC99003",
//      |      "uarn": 146440182,
//      |      "uploadedFiles": [
//      |        {
//      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
//      |          "name": "test.pdf"
//      |        },
//      |        {
//      |          "createDatetime": "2016-12-17T10:07:58.000+0000",
//      |          "name": "test.docx"
//      |        }
//      |      ]
//      |    }
//    """.stripMargin
//}
