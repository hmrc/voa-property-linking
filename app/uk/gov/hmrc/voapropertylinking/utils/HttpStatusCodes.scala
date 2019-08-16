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

package uk.gov.hmrc.voapropertylinking.utils

import scala.language.implicitConversions

/**
  * Official HTTP 1.1 status codes as defined in W3C RFC2616
  * https://www.w3.org/Protocols/rfc2616/rfc2616.html
  */
trait HttpStatusCodes {

  sealed abstract class HttpStatusCode(val code: Int) {
    val name = codeMap(code)
  }

  implicit def httpStatusCodeToInt(httpStatusCode: HttpStatusCode): Int =
    httpStatusCode.code

  def codeName(code: Int): String =
    codeMap.getOrElse(code, "")

  private val codeMap: Map[Int, String] = Map(
    100 -> "CONTINUE",
    101 -> "SWITCHING_PROTOCOLS",
    200 -> "OK",
    201 -> "CREATED",
    202 -> "ACCEPTED",
    203 -> "NON_AUTHORITATIVE_INFORMATION",
    204 -> "NO_CONTENT",
    205 -> "RESET_CONTENT",
    206 -> "PARTIAL_CONTENT",
    300 -> "MULTIPLE_CHOICES",
    301 -> "MOVED_PERMANENTLY",
    302 -> "FOUND",
    303 -> "SEE_OTHER",
    304 -> "NOT_MODIFIED",
    305 -> "USE_PROXY",
    307 -> "TEMPORARY_REDIRECT",
    400 -> "BAD_REQUEST",
    401 -> "UNAUTHORIZED",
    402 -> "PAYMENT_REQUIRED",
    403 -> "FORBIDDEN",
    404 -> "NOT_FOUND",
    405 -> "METHOD_NOT_ALLOWED",
    406 -> "NOT_ACCEPTABLE",
    407 -> "PROXY_AUTHENTICATION_REQUIRED",
    408 -> "REQUEST_TIMEOUT",
    409 -> "CONFLICT",
    410 -> "GONE",
    411 -> "LENGTH_REQUIRED",
    412 -> "PRECONDITION_FAILED",
    413 -> "REQUEST_ENTITY_TOO_LARGE",
    414 -> "REQUEST_URI_TOO_LONG",
    415 -> "UNSUPPORTED_MEDIA_TYPE",
    416 -> "REQUESTED_RANGE_NOT_SATISFIABLE",
    417 -> "EXPECTATION_FAILED",
    500 -> "INTERNAL_SERVER_ERROR",
    501 -> "NOT_IMPLEMENTED",
    502 -> "BAD_GATEWAY",
    503 -> "SERVICE_UNAVAILABLE",
    504 -> "GATEWAY_TIMEOUT",
    505 -> "HTTP_VERSION_NOT_SUPPORTED"
  )

  case object CONTINUE extends HttpStatusCode(100)
  case object SWITCHING_PROTOCOLS extends HttpStatusCode(101)
  case object OK extends HttpStatusCode(200)
  case object CREATED extends HttpStatusCode(201)
  case object ACCEPTED extends HttpStatusCode(202)
  case object NON_AUTHORITATIVE_INFORMATION extends HttpStatusCode(203)
  case object NO_CONTENT extends HttpStatusCode(204)
  case object RESET_CONTENT extends HttpStatusCode(205)
  case object PARTIAL_CONTENT extends HttpStatusCode(206)
  case object MULTIPLE_CHOICES extends HttpStatusCode(300)
  case object MOVED_PERMANENTLY extends HttpStatusCode(301)
  case object FOUND extends HttpStatusCode(302)
  case object SEE_OTHER extends HttpStatusCode(303)
  case object NOT_MODIFIED extends HttpStatusCode(304)
  case object USE_PROXY extends HttpStatusCode(305)
  case object TEMPORARY_REDIRECT extends HttpStatusCode(307)
  case object BAD_REQUEST extends HttpStatusCode(400)
  case object UNAUTHORIZED extends HttpStatusCode(401)
  case object PAYMENT_REQUIRED extends HttpStatusCode(402)
  case object FORBIDDEN extends HttpStatusCode(403)
  case object NOT_FOUND extends HttpStatusCode(404)
  case object METHOD_NOT_ALLOWED extends HttpStatusCode(405)
  case object NOT_ACCEPTABLE extends HttpStatusCode(406)
  case object PROXY_AUTHENTICATION_REQUIRED extends HttpStatusCode(407)
  case object REQUEST_TIMEOUT extends HttpStatusCode(408)
  case object CONFLICT extends HttpStatusCode(409)
  case object GONE extends HttpStatusCode(410)
  case object LENGTH_REQUIRED extends HttpStatusCode(411)
  case object PRECONDITION_FAILED extends HttpStatusCode(412)
  case object REQUEST_ENTITY_TOO_LARGE extends HttpStatusCode(413)
  case object REQUEST_URI_TOO_LONG extends HttpStatusCode(414)
  case object UNSUPPORTED_MEDIA_TYPE extends HttpStatusCode(415)
  case object REQUESTED_RANGE_NOT_SATISFIABLE extends HttpStatusCode(416)
  case object EXPECTATION_FAILED extends HttpStatusCode(417)
  case object INTERNAL_SERVER_ERROR extends HttpStatusCode(500)
  case object NOT_IMPLEMENTED extends HttpStatusCode(501)
  case object BAD_GATEWAY extends HttpStatusCode(502)
  case object SERVICE_UNAVAILABLE extends HttpStatusCode(503)
  case object GATEWAY_TIMEOUT extends HttpStatusCode(504)
  case object HTTP_VERSION_NOT_SUPPORTED extends HttpStatusCode(505)

}

object HttpStatusCodes extends HttpStatusCodes
