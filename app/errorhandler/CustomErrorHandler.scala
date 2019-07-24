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

import connectors.auth.Principal
import connectors.errorhandler.VoaClientException
import javax.inject.Inject
import play.api._
import play.api.http.HttpErrorHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results.Status
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.{BearerTokenExpired, InvalidBearerToken, MissingBearerToken}
import utils.{ErrorHandlingUtils, EventLogging, HttpStatusCodes}

import scala.collection.SortedMap
import scala.concurrent.Future
import scala.util.Try

class CustomHttpErrorHandler @Inject()() extends HttpErrorHandler with EventLogging {

  val logger: Logger = Logger(this.getClass)

  /**
    * Handles:
    *   - 4xx series exception thrown directly by Play
    */
  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    val errorResponse = statusCode match {
      case NOT_FOUND =>
        ErrorResponse.notFound("The requested URI does not exist.")
      case BAD_REQUEST =>
        ErrorResponse.badRequest("The request parameters or body are invalid. " + message)
      case REQUEST_URI_TOO_LONG =>
        ErrorResponse.requestUriTooLong
      case _ =>
        ErrorResponse(statusCode, HttpStatusCodes.codeName(statusCode), supportRequestMessage(message))
    }

    logger.warn(errorResponse.toString)

    Future.successful(Status(errorResponse.httpStatusCode)(Json.toJson(errorResponse)))
  }

  /**
    * Handles:
    *  - 5xx series exceptions that are thrown directly by Play
    *  - 4xx series exceptions thrown from the downstream modernised layer
    *  - All other exceptions that were not handled by application logic
    */
  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    implicit val unknownPrincipal: Principal = Principal("N/A", "N/A")
    val exceptionDetails = Seq( //
      "exceptionType"    -> ex.getClass.getSimpleName,
      "exceptionSummary" -> ErrorHandlingUtils.failureReason(ex),
      "requestMethod"    -> request.method)

    val errorResponse = ex match {

      case e: VoaClientException =>
        logResponse(VoaErrorOccurred, exceptionDetails: _*)
        ErrorResponse(e.responseCode, HttpStatusCodes.codeName(e.responseCode), e.message)

      case _: MissingBearerToken => ErrorResponse.unauthorized("Missing bearer token.")
      case _: BearerTokenExpired => ErrorResponse.unauthorized("The bearer token has expired.")
      case _: InvalidBearerToken => ErrorResponse.unauthorized("Invalid bearer token.")

      case _: Throwable =>
        logResponse(InternalServerErrorEvent, exceptionDetails: _*)
        ErrorResponse.internalServerError(supportRequestMessage("An unexpected exception has occurred."))
    }

    errorResponse.httpStatusCode match {
      case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE => logger.error(errorResponse.toString, ex)
      case _                                           => logger.warn(errorResponse.toString, ex)
    }

    Future.successful(Status(errorResponse.httpStatusCode)(Json.toJson(errorResponse)))
  }


  private def supportRequestMessage(message: String): String =
    s"$message " +
      "If the problem persists, please raise a support request via the Developer Hub. " +
      "https://developer.service.hmrc.gov.uk/developer/support"
}
