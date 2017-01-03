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

package models

import org.joda.time.DateTime
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.Json

class APIAuthorisationTest extends FlatSpec with MustMatchers {

  "APIAuthorisation" should "be deserialised property" in {
    val authorisation = Json.parse(DummyData.testData).as[APIAuthorisation]
    authorisation mustBe APIAuthorisation(1001, 10001, 20001, "APPROVED", "RATES_BILL", "OWNER",
      new DateTime(2016, 11, 19, 8, 35, 16),
      new DateTime(2016, 11, 1, 0, 0, 0),
      Some(new DateTime(2016, 12, 31, 0, 0, 0)),
      "ABC99001"
    )
  }
}

object DummyData {

  val testData = """
                   |    {
                   |      "id": 101,
                   |      "uarn": 1001,
                   |      "authorisationOwnerOrganisationId": 10001,
                   |      "authorisationOwnerPersonId": 20001,
                   |      "authorisationStatus": "APPROVED",
                   |      "authorisationMethod": "RATES_BILL",
                   |      "authorisationOwnerCapacity": "OWNER",
                   |      "createDateTime": "2016-11-19T08:35:16.000+0000",
                   |      "startDate": "2016-11-01T00:00:00.000+0000",
                   |      "endDate": "2016-12-31T00:00:00.000+0000",
                   |      "submissionId": "ABC99001",
                   |      "selfCertificationDeclarationFlag": true,
                   |      "parties": [
                   |        {
                   |          "id": 1,
                   |          "authorisedPartyOrganisationId": 10051,
                   |          "authorisedPartyCapacity": "AGENT",
                   |          "authorisedPartyStatus": "APPROVED",
                   |          "startDate": "2016-11-01T00:00:00.000+0000",
                   |          "endDate": "2017-03-04T00:00:00.000+0000",
                   |          "caseLinks": [],
                   |          "permissions": [
                   |            {
                   |              "id": 2,
                   |              "permissionLevel": "UPDATE",
                   |              "endDate": "2016-01-05T00:00:00.000+0000"
                   |            },
                   |            {
                   |              "id": 1,
                   |              "permissionLevel": "VIEW",
                   |              "endDate": "2016-11-01T00:00:00.000+0000"
                   |            }
                   |          ]
                   |        },
                   |        {
                   |          "id": 2,
                   |          "authorisedPartyOrganisationId": 10052,
                   |          "authorisedPartyCapacity": "AGENT",
                   |          "authorisedPartyStatus": "REVOKED",
                   |          "startDate": "2016-01-05T00:00:00.000+0000",
                   |          "endDate": "2016-04-21T23:00:00.000+0000",
                   |          "caseLinks": [
                   |            {
                   |              "id": 2,
                   |              "caseId": 98301,
                   |              "ccaCaseRef": "CCA99912346",
                   |              "startDate": "2016-05-05T23:00:00.000+0000",
                   |              "endDate": "2016-09-09T23:00:00.000+0000"
                   |            },
                   |            {
                   |              "id": 1,
                   |              "caseId": 98300,
                   |              "ccaCaseRef": "CCA99912345",
                   |              "startDate": "2016-03-04T00:00:00.000+0000",
                   |              "endDate": "2017-07-07T23:00:00.000+0000"
                   |            }
                   |          ],
                   |          "permissions": []
                   |        }
                   |      ],
                   |      "uploadedFiles": [
                   |        "supporting_evidence.docx",
                   |        "rates_bill_2016.pdf"
                   |      ],
                   |      "authorisationNotes": [
                   |        {
                   |          "id": 2,
                   |          "createdBy": "BSMITH",
                   |          "createDatetime": "2016-05-05T23:00:00.000+0000",
                   |          "content": "This is a second note"
                   |        },
                   |        {
                   |          "id": 1,
                   |          "createdBy": "ABLOGGS",
                   |          "createDatetime": "2016-03-04T00:00:00.000+0000",
                   |          "content": "This is a note"
                   |        }
                   |      ]
                   |    }
                 """.stripMargin
  }
