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

package models.mdtp.propertylink.myclients

import models.modernised.externalpropertylink.myclients.{ SummaryPropertyLinkWithClient => ModernisedSummaryPropertyLinkWithClient}
import models.searchApi.Client
import play.api.libs.json.{Json, OFormat}

case class PropertyLinkWithClient(
                                   authorisationId: Long,
                                   authorisedPartyId: Long,
                                   status: String,
                                   submissionId: String,
                                   uarn: Long,
                                   address: String,
                                   localAuthorityRef: String,
                                   client: Client,
                                   representationStatus: String
                                 )

object PropertyLinkWithClient {
  implicit val format: OFormat[PropertyLinkWithClient] = Json.format

  def apply(propertyLink: ModernisedSummaryPropertyLinkWithClient): PropertyLinkWithClient =
    PropertyLinkWithClient(
      authorisationId = propertyLink.authorisationId,
      authorisedPartyId = propertyLink.authorisedPartyId,
      representationStatus = propertyLink.representationStatus,
      status = propertyLink.status.toString,
      submissionId = propertyLink.submissionId,
      uarn = propertyLink.uarn,
      address = propertyLink.address,
      localAuthorityRef = propertyLink.localAuthorityRef,
      client = Client(propertyLink.client.organisationId, propertyLink.client.organisationName))

}
