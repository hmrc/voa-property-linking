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

import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.{AgentDetails, AppointmentChangeResponse, AppointmentChangesRequest}

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class ModernisedExternalOrganisationManagementApi @Inject() (
      httpClient: VoaHttpClient,
      @Named("voa.modernised.agentAppointmentChanges") agentAppointmentChangesUrl: String,
      @Named("voa.modernised.myAgentDetails") getAgentDetailsUrl: String
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  def agentAppointmentChanges(
        appointmentChangesRequest: AppointmentChangesRequest
  )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[AppointmentChangeResponse] =
    httpClient.postWithGgHeaders[AppointmentChangeResponse](
      url = agentAppointmentChangesUrl,
      body = Json.toJsObject(appointmentChangesRequest)
    )

  def getAgentDetails(
        agentCode: Long
  )(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[AgentDetails]] =
    httpClient.getWithGGHeaders[Option[AgentDetails]](url =
      getAgentDetailsUrl.templated("representativeCode" -> agentCode)
    )
}
