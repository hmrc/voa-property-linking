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

package errorhandler

import akka.util.Timeout
import connectors.errorhandler.VoaClientException
import org.mockito.ArgumentMatchers.{any, matches}
import org.mockito.Mockito
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Logger
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InvalidBearerToken, MissingBearerToken}
//import play.api.http.Status._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import play.api.test.Helpers._

import scala.concurrent.{ExecutionContext, Future}

class CustomHttpErrorHandlerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  //implicit val timeout = Timeout(30)

  val customHttpErrorHandler = new CustomHttpErrorHandler()

  val mockRequestHeader: RequestHeader = FakeRequest()

  "when a handling a client side error" which {

    "is a NOT_FOUND exception" should {
      "return a 404, a standard message and incident ID and log the error" in {//new Setup {
        val result = customHttpErrorHandler.onClientError(mockRequestHeader, NOT_FOUND, "Resource not found.")

        status(result) shouldBe NOT_FOUND
        (contentAsJson(result) \ "code").as[String] shouldBe "NOT_FOUND"
        (contentAsJson(result) \ "message").as[String] should endWith("The requested URI does not exist.")

        //verify(mockLogger.logger).warn(matches("\\[.{36}\\] 404 NOT_FOUND - The requested URI does not exist."))
      }
    }
  }

  "is a BAD_REQUEST exception" should {
    "return a 400, a standard message and incident ID and log the error" in {
      val result = customHttpErrorHandler.onClientError(mockRequestHeader, BAD_REQUEST, "Missing query parameter.")

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "BAD_REQUEST"
      (contentAsJson(result) \ "message").as[String] should endWith(
        "The request parameters or body are invalid. Missing query parameter.")

      //verify(mockLogger.logger).warn(matches(
        //"\\[.{36}\\] 400 BAD_REQUEST - The request parameters or body are invalid. Missing query parameter."))
    }
  }

  "is any other exception exception" should {
    "return the error code, the support message and incident ID and log the error" in {
      val result = customHttpErrorHandler.onClientError(mockRequestHeader, UNAUTHORIZED, "Unauthorised.")

      status(result) shouldBe UNAUTHORIZED
      (contentAsJson(result) \ "code").as[String] shouldBe "UNAUTHORIZED"
      (contentAsJson(result) \ "message").as[String] should endWith(
        " Unauthorised. " +
          "If the problem persists, please raise a support request via the Developer Hub. " +
          "https://developer.service.hmrc.gov.uk/developer/support")

//      verify(mockLogger.logger).warn(
//        matches(
//          "\\[.{36}\\] 401 UNAUTHORIZED - Unauthorised. " +
//            "If the problem persists, please raise a support request via the Developer Hub. " +
//            "https://developer.service.hmrc.gov.uk/developer/support"))
    }
  }

  "when a handling a server side error" which {

    "is a VoaClientException exception thrown from a Modernised" should {
      "return the status code, message and incident ID and log the error" in {

        val errorMessage = "Error message"

        val result =
          customHttpErrorHandler.onServerError(mockRequestHeader, new VoaClientException(errorMessage, BAD_REQUEST))

        status(result) shouldBe BAD_REQUEST
        (contentAsJson(result) \ "code").as[String] shouldBe "BAD_REQUEST"
        (contentAsJson(result) \ "message").as[String] should endWith(errorMessage)

        //verify(mockLogger.logger).warn(matches(s"\\[.{36}\\] 400 BAD_REQUEST - $errorMessage"), any())
      }
    }

    "is a MissingBearerToken exception thrown from auth" should {
      "return 403 UNAUTHORIZED, a standard message and incident ID and log the error" in {
        val result = customHttpErrorHandler.onServerError(mockRequestHeader, new MissingBearerToken(""))

        status(result) shouldBe UNAUTHORIZED
        (contentAsJson(result) \ "code").as[String] shouldBe "UNAUTHORIZED"
        (contentAsJson(result) \ "message").as[String] should endWith("Missing bearer token.")

        //verify(mockLogger.logger).warn(matches("\\[.{36}\\] 401 UNAUTHORIZED - Missing bearer token."), any())
      }
    }

    "is a BearerTokenExpired exception thrown from auth" should {
      "return 401 UNAUTHORIZED, a standard message and incident ID and log the error" in {
        val result = customHttpErrorHandler.onServerError(mockRequestHeader, new BearerTokenExpired(""))

        status(result) shouldBe UNAUTHORIZED
        (contentAsJson(result) \ "code").as[String] shouldBe "UNAUTHORIZED"
        (contentAsJson(result) \ "message").as[String] should endWith("The bearer token has expired.")

        //verify(mockLogger.logger).warn(matches("\\[.{36}\\] 401 UNAUTHORIZED - The bearer token has expired."), any())
      }
    }

    "is a InvalidBearerToken exception thrown from auth" should {
      "return 401 UNAUTHORIZED, a standard message and incident ID and log the error" in new {
        val result = customHttpErrorHandler.onServerError(mockRequestHeader, InvalidBearerToken(""))

        status(result) shouldBe UNAUTHORIZED
        (contentAsJson(result) \ "code").as[String] shouldBe "UNAUTHORIZED"
        (contentAsJson(result) \ "message").as[String] should endWith("Invalid bearer token.")

        //verify(mockLogger.logger).warn(matches("\\[.{36}\\] 401 UNAUTHORIZED - Invalid bearer token."), any())
      }
    }
  }
}
