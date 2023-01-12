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

package uk.gov.hmrc.voapropertylinking.controllers

import basespecs.BaseControllerSpec
import models.CanChallengeResponse
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.{JsBoolean, JsDefined}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}

import scala.concurrent.Future

class ChallengeControllerSpec extends BaseControllerSpec {

  trait Setup {
    val foo = 1L
    val controller = new ChallengeController(
      Helpers.stubControllerComponents(),
      preAuthenticatedActionBuilders(),
      mockExternalCaseManagementApi)
    val plSubmissionId = "PL12AB34"
    val canChallengeResponse = CanChallengeResponse(result = true, reasonCode = None, reason = None)
  }

  "canChallenge endpoint" should {
    "return 200 OK" when {
      "user can challenge" in new Setup {
        when(mockExternalCaseManagementApi.canChallenge(mEq(plSubmissionId), any(), any(), any())(any()))
          .thenReturn(Future.successful(Some(canChallengeResponse)))

        val result: Future[Result] =
          controller.canChallenge(plSubmissionId, "CHK123ABC", 123L, "partyID")(FakeRequest())

        status(result) shouldBe OK
        inside(contentAsJson(result) \ "result") {
          case JsDefined(JsBoolean(canChallenge)) => canChallenge shouldBe canChallengeResponse.result
        }
      }
    }
  }

}
