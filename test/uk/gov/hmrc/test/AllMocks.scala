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

package uk.gov.hmrc.test

import com.codahale.metrics.{Meter, MetricRegistry}
import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.ws.WSRequest
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.bootstrap.metrics.Metrics
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.config.{AppConfig, FeatureSwitch}
import uk.gov.hmrc.voapropertylinking.connectors.bst._
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised._
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import uk.gov.hmrc.voapropertylinking.services.{AssessmentService, PropertyLinkingService}

trait AllMocks extends MockitoSugar { me: BeforeAndAfterEach =>

  // Modernised Connectors
  val mockModernisedAddressManagementApi: ModernisedAddressManagementApi = mock[ModernisedAddressManagementApi]
  val mockModernisedCustomerManagementApi: ModernisedCustomerManagementApi = mock[ModernisedCustomerManagementApi]
  val mockModernisedCCACaseManagementApi: ModernisedCCACaseManagementApi = mock[ModernisedCCACaseManagementApi]
  val mockModernisedExternalCaseManagementApi: ModernisedExternalCaseManagementApi =
    mock[ModernisedExternalCaseManagementApi]
  val mockModernisedExternalPropertyLinkApi: ModernisedExternalPropertyLinkApi = mock[ModernisedExternalPropertyLinkApi]
  val mockModernisedExternalValuationManagementApi: ModernisedExternalValuationManagementApi =
    mock[ModernisedExternalValuationManagementApi]
  val mockModernisedOrganisationManagementApi: ModernisedExternalOrganisationManagementApi =
    mock[ModernisedExternalOrganisationManagementApi]

  // BST Connectors
  val mockAddressManagementApi: AddressManagementApi = mock[AddressManagementApi]
  val mockCustomerManagementApi: CustomerManagementApi = mock[CustomerManagementApi]
  val mockCCACaseManagementApi: CCACaseManagementApi = mock[CCACaseManagementApi]
  val mockCaseManagementApi: ExternalCaseManagementApi = mock[ExternalCaseManagementApi]
  val mockPropertyLinkApi: ExternalPropertyLinkApi = mock[ExternalPropertyLinkApi]
  val mockValuationManagementApi: ExternalValuationManagementApi = mock[ExternalValuationManagementApi]
  val mockOrganisationManagementApi: ExternalOrganisationManagementApi = mock[ExternalOrganisationManagementApi]

  // MDTP connectors
  val mockBusinessRatesAuthConnector: BusinessRatesAuthConnector = mock[BusinessRatesAuthConnector]

  val mockAssessmentService: AssessmentService = mock[AssessmentService]
  val mockPropertyLinkingService: PropertyLinkingService = mock[PropertyLinkingService]
  val mockAuditingService: AuditingService = mock[AuditingService]

  val mockDefaultHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
  val mockVoaHttpClient: VoaHttpClient = mock[VoaHttpClient]
  val mockHttpResponse: HttpResponse = mock[HttpResponse]
  val mockMeter: Meter = mock[Meter]
  val mockMetricRegistry: MetricRegistry = mock[MetricRegistry]
  val mockMetrics: Metrics = mock[Metrics]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val mockWSRequest: WSRequest = mock[WSRequest]
  val mockAppConfig: AppConfig = mock[AppConfig]

  val mockFeatureSwitch: FeatureSwitch = mock[FeatureSwitch]

  override protected def beforeEach(): Unit =
    Seq(
      mockModernisedAddressManagementApi,
      mockModernisedCustomerManagementApi,
      mockModernisedExternalCaseManagementApi,
      mockModernisedExternalPropertyLinkApi,
      mockModernisedExternalValuationManagementApi,
      mockModernisedOrganisationManagementApi,
      mockAddressManagementApi,
      mockCustomerManagementApi,
      mockCCACaseManagementApi,
      mockCaseManagementApi,
      mockPropertyLinkApi,
      mockValuationManagementApi,
      mockOrganisationManagementApi,
      mockBusinessRatesAuthConnector,
      mockAssessmentService,
      mockAuditingService,
      mockPropertyLinkingService,
      mockDefaultHttpClient,
      mockVoaHttpClient,
      mockHttpResponse,
      mockMeter,
      mockMetricRegistry,
      mockMetrics,
      mockServicesConfig,
      mockWSRequest,
      mockFeatureSwitch,
      mockAppConfig
    ).foreach(Mockito.reset(_))
}
