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

import javax.inject.{Inject, Named}
import uk.gov.hmrc.http._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.{AgentDetails, AppointmentChangeResponse, AppointmentChangesRequest}

import scala.concurrent.{ExecutionContext, Future}

class ExternalOrganisationManagementApi @Inject()(
      http: VoaHttpClient,
      @Named("voa.agentAppointmentChanges") agentAppointmentChangesUrl: String,
      @Named("voa.myAgentDetails") getAgentDetailsUrl: String
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  def agentAppointmentChanges(appointmentChangesRequest: AppointmentChangesRequest)(
        implicit hc: HeaderCarrier,
        request: RequestWithPrincipal[_]): Future[AppointmentChangeResponse] =
    http.POST[AppointmentChangesRequest, AppointmentChangeResponse](
      url = agentAppointmentChangesUrl,
      body = appointmentChangesRequest,
      headers = Seq.empty)

  def getAgentDetails(
        agentCode: Long)(implicit hc: HeaderCarrier, request: RequestWithPrincipal[_]): Future[Option[AgentDetails]] =
    http.GET[Option[AgentDetails]](url = getAgentDetailsUrl.templated("representativeCode" -> agentCode))
}
