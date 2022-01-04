/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.auditing

import basespecs.BaseUnitSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.ContentTypes
import play.api.test.FakeRequest
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class AuditingServiceSpec extends BaseUnitSpec with ContentTypes {

  implicit val request = FakeRequest()

  trait Setup {
    val mockAuditingConnector = mock[AuditConnector]

    val auditingService = new AuditingService(mockAuditingConnector)
  }

  "sending audit event" when {
    "auditing is working" should {
      "audit the extended event and return unit" in new Setup {
        when(mockAuditingConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Success))
        val result = auditingService.sendEvent("test", 999)

        result shouldBe ((): Unit)
      }
    }

    "auditing is disabled" should {
      "return unit" in new Setup {
        when(mockAuditingConnector.sendEvent(any())(any(), any())).thenReturn(Future.successful(AuditResult.Disabled))
        val result = auditingService.sendEvent("test", 999)

        result shouldBe ((): Unit)
      }

      "auditing fails" should {
        "return unit" in new Setup {
          when(mockAuditingConnector.sendEvent(any())(any(), any()))
            .thenReturn(Future.successful(AuditResult.Failure("failure")))
          val result = auditingService.sendEvent("test", 999)

          result shouldBe ((): Unit)
        }
      }
    }
  }

}
