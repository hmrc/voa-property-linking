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

import org.mockito.Mockito
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.{AssessmentService, PropertyLinkingService}
import uk.gov.hmrc.voapropertylinking.auditing.AuditingService
import uk.gov.hmrc.voapropertylinking.connectors.mdtp.BusinessRatesAuthConnector
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{AuthorisationManagementApi, _}

trait AllMocks extends MockitoSugar {
  me: BeforeAndAfterEach =>

  val mockAddressManagementApi: AddressManagementApi = mock[AddressManagementApi]
  val mockAssessmentService: AssessmentService = mock[AssessmentService]
  val mockAuditingService: AuditingService = mock[AuditingService]
  val mockAuthorisationManagementApi: AuthorisationManagementApi = mock[AuthorisationManagementApi]
  val mockAuthorisationSearchApi: AuthorisationSearchApi = mock[AuthorisationSearchApi]
  val mockBusinessRatesAuthConnector: BusinessRatesAuthConnector = mock[BusinessRatesAuthConnector]
  val mockCustomerManagementApi: CustomerManagementApi = mock[CustomerManagementApi]
  val mockMdtpDashboardManagementApi: MdtpDashboardManagementApi = mock[MdtpDashboardManagementApi]
  val mockPropertyLinkingService: PropertyLinkingService = mock[PropertyLinkingService]


  override protected def beforeEach(): Unit =
    Seq(
      mockAddressManagementApi,
      mockAssessmentService,
      mockAuditingService,
      mockAuthorisationManagementApi,
      mockAuthorisationSearchApi,
      mockBusinessRatesAuthConnector,
      mockCustomerManagementApi,
      mockMdtpDashboardManagementApi,
      mockPropertyLinkingService
    ).foreach(Mockito.reset(_))
}
