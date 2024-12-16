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
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ExternalValuationManagementStub extends WiremockMethods with WiremockHelper {

  private val baseUrl = "/external-valuation-management-api"

  def stubGetDvrDocuments(valuationId: Long, uarn: Long, propertyLinkId: String)(
        status: Int,
        body: JsValue
  ): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations/$valuationId/files\\?propertyLinkId=$propertyLinkId"
    ).thenReturn(status, body)

  def stubGetValuationHistory(uarn: Long, propertyLinkId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations\\?propertyLinkId=$propertyLinkId"
    ).thenReturn(status, body)

  def stubGetDvrDocument(valuationId: Long, uarn: Long, propertyLinkId: String, fileRef: String)(
        response: WSResponse
  )(implicit request: RequestWithPrincipal[_]): StubMapping =
    when(
      method = GET,
      uri = s"$baseUrl/properties/$uarn/valuations/$valuationId/files/$fileRef\\?propertyLinkId=$propertyLinkId",
      headers = Map(
        "GG-EXTERNAL-ID" -> request.principal.externalId,
        "GG-GROUP-ID"    -> request.principal.groupId,
        "User-Agent"     -> "voa-property-linking"
      )
    ).thenReturn(response.status, response.body)
}
