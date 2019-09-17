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

package uk.gov.hmrc.test

import com.codahale.metrics.{Meter, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.ws.{StreamedResponse, WSRequest, WSResponseHeaders}
import services.{AssessmentService, PropertyLinkingService}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised._
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

trait AllMocks extends MockitoSugar {
  me: BeforeAndAfterEach =>

  val mockAddressManagementApi: AddressManagementApi = mock[AddressManagementApi]
  val mockAssessmentService: AssessmentService = mock[AssessmentService]
  val mockAuditingService: AuditingService = mock[AuditingService]
  val mockAuthorisationManagementApi: AuthorisationManagementApi = mock[AuthorisationManagementApi]
  val mockAuthorisationSearchApi: AuthorisationSearchApi = mock[AuthorisationSearchApi]
  val mockBusinessRatesAuthConnector: BusinessRatesAuthConnector = mock[BusinessRatesAuthConnector]
  val mockCustomerManagementApi: CustomerManagementApi = mock[CustomerManagementApi]
  val mockDefaultHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockExternalPropertyLinkApi: ExternalPropertyLinkApi = mock[ExternalPropertyLinkApi]
  val mockExternalValuationManagementApi: ExternalValuationManagementApi = mock[ExternalValuationManagementApi]
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val mockMdtpDashboardManagementApi: MdtpDashboardManagementApi = mock[MdtpDashboardManagementApi]
  val mockMeter: Meter = mock[Meter]
  val mockMetricRegistry: MetricRegistry = mock[MetricRegistry]
  val mockMetrics: Metrics = mock[Metrics]
  val mockPropertyLinkingService: PropertyLinkingService = mock[PropertyLinkingService]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockStreamedResponse: StreamedResponse = mock[StreamedResponse]
  val mockVoaHttpClient: VoaHttpClient = mock[VoaHttpClient]
  val mockWSRequest: WSRequest = mock[WSRequest]
  val mockWSResponseHeaders: WSResponseHeaders = mock[WSResponseHeaders]

  override protected def beforeEach(): Unit =
    Seq(
      mockAddressManagementApi,
      mockAssessmentService,
      mockAuditingService,
      mockAuthorisationManagementApi,
      mockAuthorisationSearchApi,
      mockBusinessRatesAuthConnector,
      mockCustomerManagementApi,
      mockDefaultHttpClient,
      mockExternalPropertyLinkApi,
      mockExternalValuationManagementApi,
      mockHttpResponse,
      mockMdtpDashboardManagementApi,
      mockMeter,
      mockMetricRegistry,
      mockMetrics,
      mockPropertyLinkingService,
      mockServicesConfig,
      mockStreamedResponse,
      mockVoaHttpClient,
      mockWSRequest,
      mockWSResponseHeaders
    ).foreach(Mockito.reset(_))
}
