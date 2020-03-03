/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import java.time.{Clock, Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}

import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.Capacity.{Capacity => _, _}
import models.modernised.{Capacity, Evidence, EvidenceType, ProvidedEvidence}
import models.modernised.ProvidedEvidence.{apply => _, _}
import models.modernised.externalpropertylink.myorganisations.{AgentDetails, AgentList, AgentSummary}
import models.{FileInfo, GroupAccount, GroupAccountSubmission, GroupId, IndividualAccount, IndividualAccountSubmission, IndividualAccountSubmissionForOrganisation, IndividualDetails}
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import play.api.libs.json.Json
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.{AgentOrganisation, AppointmentChangeResponse, OrganisationLatestDetail}

trait FakeObjects {

  val date = LocalDate.parse("2018-09-05")
  val today = LocalDate.now()
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
  val FILE_NAME = "test.pdf"
  val fileInfo = FileInfo(FILE_NAME, "ratesBill")
  val evidence = Evidence(FILE_NAME, EvidenceType.RATES_BILL)
  val apiPropertyLinkRequest = APIPropertyLinkRequest(
    uarn = 11111,
    authorisationOwnerOrganisationId = 2222,
    authorisationOwnerPersonId = 33333,
    createDatetime = Instant.now(),
    authorisationMethod = "RATES_BILL",
    uploadedFiles = Seq(fileInfo),
    submissionId = "44444",
    authorisationOwnerCapacity = "OWNER",
    startDate = date,
    endDate = Some(date)
  )

  val testCreatePropertyLink: CreatePropertyLink = CreatePropertyLink(
    uarn = 11111,
    capacity = Capacity.withName("OWNER"),
    startDate = date,
    endDate = Some(date),
    method = ProvidedEvidence.withName("RATES_BILL"),
    PLsubmissionId = "44444",
    createDatetime = LocalDateTime.now(),
    uploadedFiles = Seq(evidence),
    submissionSource = "DFE_UI"
  )

  val groupAccountSubmission: GroupAccountSubmission = GroupAccountSubmission(
    id = "acc123",
    companyName = "Real news Inc",
    addressId = 9876543L,
    email = "thewhitehouse@potus.com",
    phone = "01987654",
    isAgent = false,
    individualAccountSubmission = IndividualAccountSubmissionForOrganisation(
      externalId = "Ext123",
      trustId = "trust234",
      details = IndividualDetails(
        firstName = "Donald",
        lastName = "Trump",
        email = "therealdonald@potus.com",
        phone1 = "123456789",
        phone2 = Some("987654321"),
        addressId = 24680L
      )
    )
  )

  val groupId =
    GroupId(id = 654321L, message = "valid group id", responseTime = 45678)

  val someGroupAccount = Some(
    GroupAccount(
      id = 2,
      groupId = "gggId",
      companyName = "Fake News Inc",
      addressId = 345,
      email = "therealdonald@potus.com",
      phone = "9876541",
      isAgent = false,
      agentCode = None))

  val individualAccountSubmission = IndividualAccountSubmission(
    externalId = "ggEId12",
    trustId = "idv1",
    organisationId = 13579,
    details = IndividualDetails(
      firstName = "Kim",
      lastName = "Yong Un",
      email = "thechosenone@nkorea.nk",
      phone1 = "24680",
      phone2 = Some("13579"),
      addressId = 9876
    )
  )

  val expectedGetValidResponse = Some(
    IndividualAccount(
      externalId = "ggEId12",
      trustId = "idv1",
      organisationId = 13579,
      individualId = 2,
      details = IndividualDetails(
        firstName = "anotherFirstName",
        lastName = "anotherLastName",
        email = "theFakeDonald@potus.com",
        phone1 = "24680",
        phone2 = Some("13579"),
        addressId = 9876
      )
    ))

  val expectedGetEmptyResponse = None

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  val expectedUpdateValidResponse = Json.parse("""{
    "id": 2,
    "governmentGatewayExternalId": "ggEId12",
    "personLatestDetail": {
                                                 |"addressUnitId": 9876,
                                                 |"firstName": "anotherFirstName",
                                                 |"lastName": "anotherLastName",
                                                 |"emailAddress": "theFakeDonald@potus.com",
                                                 |"telephoneNumber": "24680",
                                                 |"mobileNumber": "13579",
                                                 |"identifyVerificationId": "idv1"
    },
    "organisationId": 13579,
    "organisationLatestDetail": {
      "addressUnitId": 345,
      "representativeFlag": false,
      "organisationName": "Fake News Inc",
      "organisationEmailAddress": "therealdonald@potus.com",
      "organisationTelephoneNumber": "9876541"
      }
  }""".stripMargin)

  val agentCode = 12345L

  val agentOrganisation = AgentOrganisation(
    id = 12L,
    representativeCode = Some(agentCode),
    organisationLatestDetail = OrganisationLatestDetail(
      id = 1L,
      addressUnitId = 1L,
      organisationName = "An Org",
      organisationEmailAddress = "some@email.com",
      organisationTelephoneNumber = "0456273893232",
      representativeFlag = true
    ),
    persons = List()
  )

  val agentDetails =
    agentrepresentation.AgentDetails(name = "Super Agent", address = "123 Super Agent Street, AA1 1AA")

  val groupAccount = GroupAccount(
    id = 2,
    groupId = "gggId",
    companyName = "Fake News Inc",
    addressId = 345,
    email = "therealdonald@potus.com",
    phone = "9876541",
    isAgent = false,
    agentCode = Some(234)
  )

  val agentSummary = AgentSummary(
    organisationId = 1L,
    representativeCode = 987L,
    name = "Some Agent Org",
    appointedDate = LocalDate.now().minusDays(1),
    propertyCount = 2
  )
  val organisationsAgentsList = AgentList(resultCount = 1, agents = List(agentSummary))
  val emptyOrganisationsAgentsList = AgentList(resultCount = 0, agents = List.empty)
  val appointmentChangeResponse = AppointmentChangeResponse(appointmentChangeId = "appointment change id")

}
