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

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Results
import utils.HttpStatusCodes

case class ErrorResponse(
                          httpStatusCode: Int,
                          errorCode: String,
                          message: String,
                          errors: Option[Seq[NestedError]] = None,
                          incidentId: String = UUID.randomUUID().toString) {

  override def toString: String =
    s"[$incidentId] $httpStatusCode $errorCode - $message"

  lazy val json = Json.toJson(this)(ErrorResponse.writes)

}


object ErrorResponse extends HttpStatusCodes with Results {

  def apply(httpStatusCode: HttpStatusCode, message: String): ErrorResponse =
    ErrorResponse(httpStatusCode.code, httpStatusCode.name, message)

  def apply(httpStatusCode: HttpStatusCode, message: String, nestedErrors: Seq[NestedError]): ErrorResponse =
    ErrorResponse(httpStatusCode.code, httpStatusCode.name, message, Some(nestedErrors))

  implicit val writes = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue =
      Json
        .obj("code" -> e.errorCode, "message" -> s"[${e.incidentId}] ${e.message}")
        .deepMerge(e.errors.map(details => Json.obj("errors" -> details)).getOrElse(Json.obj()))
  }

  /* 400 */
  def badRequest(message: String) =
    ErrorResponse(BAD_REQUEST, message)
  def badRequestJsonResult(message: String) =
    BadRequest(ErrorResponse(BAD_REQUEST, message).json)
  def badRequestJsonResult(message: String, nestedErrors: Seq[NestedError]) =
    BadRequest(ErrorResponse(BAD_REQUEST, message, nestedErrors).json)

  /* 403 */
  def unauthorized(message: String) =
    ErrorResponse(UNAUTHORIZED, message)
  def unauthorizedJsonResult(message: String) =
    Unauthorized(ErrorResponse(UNAUTHORIZED, message).json)

  /* 404 */
  def notFound(message: String) =
    ErrorResponse(NOT_FOUND, message)
  def notFoundJsonResult(message: String) =
    NotFound(ErrorResponse(NOT_FOUND, message).json)

  /* 414 */
  val requestUriTooLong =
    ErrorResponse(REQUEST_URI_TOO_LONG, "The request URI is too long.")

  /* 500 */
  def internalServerError(message: String) =
    ErrorResponse(INTERNAL_SERVER_ERROR, message)
  def internalServerErrorJsonResult(message: String) =
    InternalServerError(ErrorResponse(INTERNAL_SERVER_ERROR, message).json)

  /* 501 */
  def notImplemented(message: String) =
    ErrorResponse(NOT_IMPLEMENTED, message)
  def notImplementedJsonResult(message: String) =
    NotImplemented(ErrorResponse(NOT_IMPLEMENTED, message).json)

}
