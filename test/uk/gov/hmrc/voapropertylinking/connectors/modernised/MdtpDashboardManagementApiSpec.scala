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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import java.time.LocalDate

import basespecs.BaseUnitSpec
import models._
import models.modernised.ValuationHistory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._

import scala.concurrent.Future

class MdtpDashboardManagementApiSpec extends BaseUnitSpec {

  trait Setup {
    val connector = new MdtpDashboardManagementApi(mockDefaultHttpClient, mockServicesConfig)
    val authorisationId = 1L

    val valuationHistory = ValuationHistory(
      asstRef = 125689,
      listYear = "2017",
      uarn = 923411,
      billingAuthorityReference = "VOA1",
      address = "123 Foo bar, Some adDREss, cItY, Bn2 2bw",
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
      billingAuthorityCode = None)

    val propertiesView = PropertiesView(
      authorisationId = authorisationId,
      uarn = 123L,
      authorisationStatus = "OPEN",
      startDate = today,
      endDate = Option.empty[LocalDate],
      submissionId = "PL123",
      address = Some("Some Address that was returned"),
      NDRListValuationHistoryItems = Seq(APIValuationHistory(valuationHistory)),
      parties = Seq.empty[APIParty],
      agents = Option.empty[Seq[LegacyParty]]
    )
  }

  "dashboard management API connector" should {
    "return a PropertiesView with address in history valuation upper-cased" when {
      "modernised returns it" in new Setup {
        when(mockDefaultHttpClient.GET[Option[PropertiesView]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Some(propertiesView)))

        inside(connector.get(authorisationId).futureValue) {
          case Some(PropertiesView(_, _, _, _, _, _, _, valuationHistories, _, _)) =>
            valuationHistories.loneElement.address shouldBe upperCased
        }
      }
    }
    "return None" when {
      "modernised doesn't return anything" in new Setup {
        when(mockDefaultHttpClient.GET[Option[PropertiesView]](any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(Option.empty[PropertiesView]))

        connector.get(authorisationId).futureValue shouldBe None
      }
    }

  }

}
