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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import scala.concurrent.Future

class ModernisedCCACaseManagementApiSpec extends BaseUnitSpec {

  val http: VoaHttpClient = mock[VoaHttpClient]
  val connector: ModernisedCCACaseManagementApi =
    new ModernisedCCACaseManagementApi(http, mockAppConfig) {
      override lazy val url: String = "http://some-url"
    }

  "request detailed valuation" should {
    "update the valuation with the detailed valuation request" in {

      when(http.postWithGgHeaders[HttpResponse](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(emptyJsonHttpResponse(200)))

      val dvr = DetailedValuationRequest(
        authorisationId = 123456,
        organisationId = 9876543,
        personId = 1111111,
        submissionId = "submission1",
        assessmentRef = 24680,
        agents = None,
        billingAuthorityReferenceNumber = "barn1"
      )

      connector.requestDetailedValuation(dvr).futureValue shouldBe ((): Unit)
    }
  }
}
