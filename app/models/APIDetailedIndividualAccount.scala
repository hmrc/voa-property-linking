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

import org.joda.time.LocalDate
import play.api.libs.json._

case class APIDetailedIndividualAccount(id: Int, governmentGatewayExternalId: String, personLatestDetail: APIIndividualDetails,
                                        organisationId: Int, organisationLatestDetail: GroupDetails) {

  def toIndividualAccount(address: SimpleAddress) = {
    IndividualAccount(governmentGatewayExternalId, personLatestDetail.identifyVerificationId, organisationId, id,
      IndividualDetails(personLatestDetail.firstName, personLatestDetail.lastName, personLatestDetail.emailAddress,
        personLatestDetail.telephoneNumber.getOrElse("not set"), personLatestDetail.mobileNumber, address)
    )
  }
}

case class APIIndividualDetails(addressUnitId: Int, firstName: String, lastName: String, emailAddress: String, telephoneNumber: Option[String],
                                mobileNumber: Option[String], identifyVerificationId: String, effectiveFrom: LocalDate)

object APIIndividualDetails {
  private def withDefault[A](key: String, default: A)(implicit wrts: Writes[A]): Reads[JsObject] = {
    __.json.update((__ \ key).json.copyFrom((__ \ key).json.pick orElse Reads.pure(Json.toJson(default))))
  }

  implicit val format = new Format[APIIndividualDetails] {
    override def writes(o: APIIndividualDetails) = Json.writes[APIIndividualDetails].writes(o)

    override def reads(json: JsValue) = Json.reads[APIIndividualDetails].compose(withDefault("identifyVerificationId", "")).reads(json)
  }
}

object APIDetailedIndividualAccount {
  implicit val format = Json.format[APIDetailedIndividualAccount]
}
