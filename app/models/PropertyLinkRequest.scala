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

package models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class PropertyLinkRequest(uarn: Long, organisationId: Int, individualId: Int,
                               capacityDeclaration: CapacityDeclaration,
                               linkedDate: DateTime, linkBasis: String,
                               specialCategoryCode: String, description: String, bulkClassIndicator: String,
                               fileInfo: Option[FileInfo])

object PropertyLinkRequest {
  implicit val propertyLinkRequest = Json.format[PropertyLinkRequest]
}