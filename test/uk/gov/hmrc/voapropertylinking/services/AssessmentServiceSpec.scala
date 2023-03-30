/*
 * Copyright 2023 HM Revenue & Customs
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
    val mockModernisedPropertyLinkApi: ModernisedExternalPropertyLinkApi = mock[ModernisedExternalPropertyLinkApi]
    val mockModernisedValuationManagementApi: ModernisedExternalValuationManagementApi =
      mock[ModernisedExternalValuationManagementApi]

    val date: LocalDate = LocalDate.parse("2018-09-05")
    val uarn: Long = 33333L
    val submissionId: String = "PL123ABC"

    val propertyLinkWithAgents: PropertyLinkWithAgents =
      PropertyLinkWithAgents(
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

    val propertyLinkWithClient: PropertyLinkWithClient =
      PropertyLinkWithClient(
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

    val ownerPropertyLink: OwnerPropertyLink = OwnerPropertyLink(propertyLinkWithAgents)
    val clientPropertyLink: ClientPropertyLink = ClientPropertyLink(propertyLinkWithClient)

    val valuationHistory: ValuationHistory =
      ValuationHistory(
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
        listType = ListType.CURRENT,
        allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION)
      )

    val assessmentService: AssessmentService =
      new AssessmentService(
        mockModernisedPropertyLinkApi,
        mockModernisedValuationManagementApi,
        mockPropertyLinkApi,
        mockValuationManagementApi,
        mockFeatureSwitch
      )
  }

  "If the bstDownstream feature switch is disabled" when {

    "getMyOrganisationsAssessments" should {
      "return assessments" in new Setup {
        when(mockModernisedPropertyLinkApi.getMyOrganisationsPropertyLink(mEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(ownerPropertyLink)))
        when(mockModernisedValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
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
        when(mockModernisedPropertyLinkApi.getClientsPropertyLink(mEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(clientPropertyLink)))
        when(mockModernisedValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
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

  "If the bstDownstream feature switch is enabled" when {

    "getMyOrganisationsAssessments" should {
      "return assessments" in new Setup {
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getMyOrganisationsPropertyLink(mEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(ownerPropertyLink)))
        when(mockValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
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
        when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
        when(mockPropertyLinkApi.getClientsPropertyLink(mEq(submissionId))(any()))
          .thenReturn(Future.successful(Some(clientPropertyLink)))
        when(mockValuationManagementApi.getValuationHistory(mEq(uarn), mEq(submissionId))(any()))
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
}
