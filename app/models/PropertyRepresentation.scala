/*
 * Copyright 2016 HM Revenue & Customs
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

import play.api.libs.json.Json

case class PropertyRepresentation(representationId: String, linkId: String, agentId: String, agentName: String, groupId: String,
                                  groupName: String, uarn: Long, canCheck: String, canChallenge: String, pending: Boolean) {

  def withAddress(address: Address) = DetailedPropertyRepresentation(
    representationId, linkId, agentId, agentName, groupId, groupName, uarn, address, canCheck, canChallenge, pending
  )
}

object PropertyRepresentation {
  implicit val propertyRepresentationFormat = Json.format[PropertyRepresentation]
}
