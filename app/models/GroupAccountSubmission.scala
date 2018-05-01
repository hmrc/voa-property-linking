/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.{Clock, Instant}

import play.api.libs.json.Json

case class GroupAccountSubmission(id: String, companyName: String, addressId: Long, email: String, phone: String,
                                  isAgent: Boolean, individualAccountSubmission: IndividualAccountSubmissionForOrganisation) {

  def toApiAccount(implicit clock: Clock): APIGroupAccountSubmission = {
    APIGroupAccountSubmission(id, companyName, addressId, email, phone, isAgent, None, Instant.now(clock),
      APIIndividualAccountForOrganisation(
        individualAccountSubmission.trustId,
        individualAccountSubmission.details.firstName,
        individualAccountSubmission.details.lastName,
        individualAccountSubmission.details.addressId,
        individualAccountSubmission.details.phone1,
        individualAccountSubmission.details.phone2,
        individualAccountSubmission.details.email,
        individualAccountSubmission.externalId,
        Instant.now(clock))
    )
  }
}

object GroupAccountSubmission {
  implicit val formats = Json.format[GroupAccountSubmission]
}
