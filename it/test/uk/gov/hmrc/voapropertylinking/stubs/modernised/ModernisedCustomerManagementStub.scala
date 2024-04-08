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

package uk.gov.hmrc.voapropertylinking.stubs.modernised

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait ModernisedCustomerManagementStub extends WiremockMethods with WiremockHelper {

  private val organisationUrl = "/customer-management-api/organisation"
  private val individualUrl = "/customer-management-api/person"

  //Organisations
  def stubCreateGroupAccount(account: JsValue)(status: Int, body: JsValue): StubMapping =
    when(
      method = POST,
      uri = organisationUrl,
      body = account
    ).thenReturn(status, body)

  def stubUpdateGroupAccount(orgId: Long, account: JsValue)(response: HttpResponse): StubMapping =
    when(
      method = PUT,
      uri = s"$organisationUrl/$orgId",
      body = account
    ).thenReturn(response.status, response.json)

  def stubGetDetailedGroupAccount(id: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$organisationUrl\\?organisationId=$id"
    ).thenReturn(status, body)

  def stubFindDetailedGroupAccountByGGID(ggId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$organisationUrl\\?governmentGatewayGroupId=$ggId"
    ).thenReturn(status, body)

  def stubWithAgentCode(agentCode: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$organisationUrl\\?representativeCode=$agentCode"
    ).thenReturn(status, body)

  //Individuals
  def stubCreateIndividualAccount(account: JsValue)(status: Int, body: JsValue): StubMapping =
    when(
      method = POST,
      uri = s"$individualUrl",
      body = account
    ).thenReturn(status, body)

  def stubUpdateIndividualAccount(personId: Long, account: JsValue)(status: Int, body: JsValue): StubMapping =
    when(
      method = PUT,
      uri = s"$individualUrl/$personId",
      body = account
    ).thenReturn(status, body)

  def stubGetDetailedIndividual(id: Long)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$individualUrl\\?personId=$id"
    ).thenReturn(status, body)

  def stubFindDetailedIndividualAccountByGGID(ggId: String)(status: Int, body: JsValue): StubMapping =
    when(
      method = GET,
      uri = s"$individualUrl\\?governmentGatewayExternalId=$ggId"
    ).thenReturn(status, body)
}
