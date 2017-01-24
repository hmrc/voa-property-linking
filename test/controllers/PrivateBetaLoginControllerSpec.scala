/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import config.AppConfig
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import repositories.FailedLogin
import utils.StubLoginAttemptsRepo

class PrivateBetaLoginControllerSpec extends ControllerSpec {

  private object TestPrivateBetaLoginController extends PrivateBetaLoginController {
    override val repo = StubLoginAttemptsRepo
    override val config = new AppConfig {
      override val passwordValidationEnabled = true
      override val maxAttempts = 5
      override val privateBetaPassword = "CCAisFun"
      override val lockoutHours = 24
    }
  }

  val testIp = "test-ip"
  val validHeaders = FakeHeaders(Seq("Content-Type" -> "application/json", "True-Client-IP" -> testIp))
  val validRequest = FakeRequest("POST", "/", validHeaders, Json.obj("password" -> "CCAisFun"))
  val incorrectPasswordRequest = FakeRequest("POST", "/", validHeaders, Json.obj("password" -> "CCAisNotFun"))

  "Logging in to private beta with the correct password for the first time" should {
    "return a 200 OK status" in {
      val res = TestPrivateBetaLoginController.login()(validRequest)
      status(res) mustBe OK
    }
  }

  "Logging in to private beta with the wrong password for the first time" should {
    "return a 401 Unauthorised status and record the failure in mongo" in {
      val res = TestPrivateBetaLoginController.login()(incorrectPasswordRequest)
      status(res) mustBe UNAUTHORIZED

      await(StubLoginAttemptsRepo.mostRecent(testIp, 5, DateTime.now.minusHours(24))) must have size 1
    }
  }

  "Logging in to private beta with the wrong password twice" should {
    "return a 401 Unauthorised status both times and record both failures" in {
      (1 to 2) foreach { _ =>
        val res = TestPrivateBetaLoginController.login()(incorrectPasswordRequest)
        status(res) mustBe UNAUTHORIZED
      }

      await(StubLoginAttemptsRepo.mostRecent(testIp, 5, DateTime.now.minusHours(24))) must have size 2
    }
  }

  "Logging in to private beta with the wrong password 6 times" should {
    "lock the user out, and not allow the user to log in, even if they enter the correct password" in {
      (1 to 5) foreach { _ =>
        val res = TestPrivateBetaLoginController.login()(incorrectPasswordRequest)
        status(res) mustBe UNAUTHORIZED
      }

      val res = TestPrivateBetaLoginController.login()(incorrectPasswordRequest)
      status(res) mustBe UNAUTHORIZED
      contentAsJson(res) mustBe Json.obj("errorCode" -> "LOCKED_OUT")

      val res2 = TestPrivateBetaLoginController.login()(validRequest)
      status(res2) mustBe UNAUTHORIZED
      contentAsJson(res2) mustBe Json.obj("errorCode" -> "LOCKED_OUT")
    }
  }

  "Logging in to private beta when the true client IP header is not present" should {
    "return a 400 BAD_REQUEST status" in {
      val missingIPHeader = FakeRequest("POST", "/", FakeHeaders(Seq("Content-Type" -> "application/json")), Json.obj("password" -> "CCAisFun"))
      val res = TestPrivateBetaLoginController.login()(missingIPHeader)
      status(res) mustBe BAD_REQUEST
    }
  }

  "Logging in with the correct password, after being locked out, but after the lockout period has ended" should {
    "return a 200 OK status" in {
      StubLoginAttemptsRepo.stubData(
        Seq(
          FailedLogin(DateTime.now.minusHours(24), testIp),
          FailedLogin(DateTime.now.minusHours(25), testIp),
          FailedLogin(DateTime.now.minusHours(26), testIp),
          FailedLogin(DateTime.now.minusHours(27), testIp),
          FailedLogin(DateTime.now.minusHours(28), testIp)
        )
      )

      val res = TestPrivateBetaLoginController.login()(validRequest)
      status(res) mustBe OK
    }
  }
}
