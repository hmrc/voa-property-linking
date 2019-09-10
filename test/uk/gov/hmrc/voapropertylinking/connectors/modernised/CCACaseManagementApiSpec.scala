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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models.voa.valuation.dvr.DetailedValuationRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class CCACaseManagementApiSpec extends BaseUnitSpec {

  val http = mock[DefaultHttpClient]
  val connector = new CCACaseManagementApi(http, mock[ServicesConfig]) {
    override lazy val baseURL: String = "http://some-url"
  }

  "request detailed valuation" should {
    "update the valuation with the detailed valuation request" in {

      when(http.POST[DetailedValuationRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      val dvr = DetailedValuationRequest(
        authorisationId = 123456,
        organisationId = 9876543,
        personId = 1111111,
        submissionId = "submission1",
        assessmentRef = 24680,
        agents = None,
        billingAuthorityReferenceNumber = "barn1"
      )

      connector.requestDetailedValuation(dvr).futureValue shouldBe (())
    }
  }
}
