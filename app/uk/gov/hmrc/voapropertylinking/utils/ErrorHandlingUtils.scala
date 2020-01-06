/*
 * Copyright 2020 HM Revenue & Customs
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

import org.apache.commons.lang3.exception.ExceptionUtils.getStackFrames

object ErrorHandlingUtils {

  val LINES_OF_STACK_TRACE = 5

  def failureReason(t: Throwable): String =
    Seq(
      Some("Message: " + t.getMessage),
      Option(t.getCause).map("Cause: " + _.getMessage),
      Some("Stack Trace: " + getStackFrames(t).take(LINES_OF_STACK_TRACE).mkString("\n"))
    ).flatten.mkString("\n")

}
