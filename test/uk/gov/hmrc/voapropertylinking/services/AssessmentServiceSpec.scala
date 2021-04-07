/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.services

import java.time.LocalDate

import basespecs.BaseUnitSpec
import cats.data.OptionT
import models.Assessments
import models.modernised._
import models.modernised.externalpropertylink.myclients.{ClientDetails, ClientPropertyLink, PropertyLinkWithClient}
import models.modernised.externalpropertylink.myorganisations._
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import uk.gov.hmrc.voapropertylinking.connectors.modernised._

import scala.concurrent.Future

class AssessmentServiceSpec extends BaseUnitSpec {

  private trait Setup {
    val mockExternalPropertyLinkApi: ExternalPropertyLinkApi = mock[ExternalPropertyLinkApi]
    val mockExternalValuationManagementApi: ExternalValuationManagementApi = mock[ExternalValuationManagementApi]

    val date: LocalDate = LocalDate.parse("2018-09-05")
    val uarn: Long = 33333L
    val submissionId: String = "PL123ABC"

    val propertyLinkWithAgents = PropertyLinkWithAgents(
      authorisationId = 11111,
      status = PropertyLinkStatus.APPROVED,
      startDate = date,
      endDate = Some(date),
      submissionId = submissionId,
      uarn = uarn,
      address = "1 HIGH STREET, BRIGHTON",
      localAuthorityRef = "44444",
      agents = Seq(
        AgentDetails(
          authorisedPartyId = 24680,
          organisationId = 123456,
          organisationName = "org name",
          representativeCode = 1111
        )),
      capacity = "OWNER"
    )

    val propertyLinkWithClient = PropertyLinkWithClient(
      authorisationId = 11111,
      authorisedPartyId = 11111,
      status = PropertyLinkStatus.APPROVED,
      startDate = date,
      endDate = Some(date),
      submissionId = submissionId,
      capacity = "OWNER",
      uarn = uarn,
      address = "1 HIGH STREET, BRIGHTON",
      localAuthorityRef = "44444",
      client = ClientDetails(55555, "mock org")
    )

    val ownerPropertyLink = OwnerPropertyLink(propertyLinkWithAgents)
    val clientPropertyLink = ClientPropertyLink(propertyLinkWithClient)

    val valuationHistory = ValuationHistory(
      asstRef = 125689,
      listYear = "2017",
      uarn = 923411,
      billingAuthorityReference = "VOA1",
      address = "1 HIGH STREET, BRIGHTON",
      description = None,
      specialCategoryCode = None,
      compositeProperty = None,
      effectiveDate = Some(date),
      listAlterationDate = None,
      numberOfPreviousProposals = None,
      settlementCode = None,
      totalAreaM2 = None,
      costPerM2 = None,
      rateableValue = Some(2599),
      transitionalCertificate = None,
      deletedIndicator = None,
      valuationDetailsAvailable = None,
      billingAuthCode = None,
      listType = ListType.CURRENT
    )

    val assessmentService = new AssessmentService(mockExternalPropertyLinkApi, mockExternalValuationManagementApi)
  }

  "getMyOrganisationsAssessments" should {
    "return assessments" in new Setup {
      when(mockExternalPropertyLinkApi.getMyOrganisationsPropertyLink(mEq(submissionId))(any()))
        .thenReturn(Future.successful(Some(ownerPropertyLink)))
      when(mockExternalValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
        .thenReturn(Future.successful(Some(ValuationHistoryResponse(Seq(valuationHistory)))))

      val res: OptionT[Future, Assessments] = assessmentService.getMyOrganisationsAssessments(submissionId)

      val expectedAssessments: Assessments = Assessments(
        propertyLink = propertyLinkWithAgents,
        history = Seq(valuationHistory),
        capacity = Some("OWNER")
      )

      res.value.futureValue shouldBe Some(expectedAssessments)
    }
  }

  "getClientsAssessments" should {
    "return assessments" in new Setup {
      when(mockExternalPropertyLinkApi.getClientsPropertyLink(mEq(submissionId))(any()))
        .thenReturn(Future.successful(Some(clientPropertyLink)))
      when(mockExternalValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
        .thenReturn(Future.successful(Some(ValuationHistoryResponse(Seq(valuationHistory)))))

      val res: OptionT[Future, Assessments] = assessmentService.getClientsAssessments(submissionId)

      val expectedAssessments: Assessments = Assessments(
        propertyLink = propertyLinkWithClient,
        history = Seq(valuationHistory),
        capacity = Some("OWNER")
      )

      res.value.futureValue shouldBe Some(expectedAssessments)
    }
  }

}
