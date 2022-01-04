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

package models.modernised

import uk.gov.hmrc.voapropertylinking.utils.JsonUtils.enumFormat

object PropertyLinkStatus extends Enumeration {
  type PropertyLinkStatus = Value

  val APPROVED = Value("APPROVED")
  val PENDING = Value("PENDING")
  val DECLINED = Value("DECLINED")
  val REVOKED = Value("REVOKED")
  val MORE_EVIDENCE_REQUIRED = Value("MORE_EVIDENCE_REQUIRED")

  implicit val format = enumFormat(PropertyLinkStatus)
}
