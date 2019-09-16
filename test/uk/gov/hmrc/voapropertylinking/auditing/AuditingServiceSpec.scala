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

package uk.gov.hmrc.voapropertylinking.auditing

import basespecs.WireMockSpec
import helpers.SimpleWsHttpTestApplication
import play.api.http.ContentTypes
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class AuditingServiceSpec
  extends WireMockSpec
    with ContentTypes
    with SimpleWsHttpTestApplication {

  implicit val request = FakeRequest()

  "AuditingService.sendEvent" should {
    "audit the extended event" in {
      fakeApplication.injector.instanceOf[AuditingService].sendEvent[Int]("test", 999) shouldBe (())
    }
  }

}
