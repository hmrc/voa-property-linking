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

package models.modernised

import play.api.libs.json.Format
import utils.JsonUtils.enumFormat

object ApiVersion extends Enumeration {

  type ApiVersion = Value

  val VERSION_1_0 = Value("1.0")
  val VERSION_1_1 = Value("1.1")
  val VERSION_1_2 = Value("1.2")

  implicit val format: Format[ApiVersion] = enumFormat(ApiVersion)

}
