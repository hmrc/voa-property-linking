/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ExternalCaseManagementStub extends WiremockMethods with WiremockHelper {

  private val baseUrl = "/external-case-management-api/my-organisation"
  def stubGetMyOrganisationCheckCases(propertyLinkSubmissionId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/property-links/$propertyLinkSubmissionId/check-cases\\?start=1&size=100"
    ).thenReturn(status, body)

  def stubGetMyClientsCheckCases(propertyLinkSubmissionId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/clients/all/property-links/$propertyLinkSubmissionId/check-cases\\?start=1&size=100"
    ).thenReturn(status, body)

  def stubCanChallengeClient(propertyLinkSubmissionId: String, checkCaseRef: String, valuationId: Long)(
        status: Int,
        body: JsValue
  ): StubMapping =
    when(
      method = GET,
      uri =
        s"$baseUrl/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge\\?valuationId=$valuationId"
    ).thenReturn(status, body)

  def stubCanChallengeAgent(propertyLinkSubmissionId: String, checkCaseRef: String, valuationId: Long)(
        status: Int,
        body: JsValue
  ): StubMapping =
    when(
      method = GET,
      uri =
        s"$baseUrl/clients/all/property-links/$propertyLinkSubmissionId/check-cases/$checkCaseRef/canChallenge\\?valuationId=$valuationId"
    ).thenReturn(status, body)

}
