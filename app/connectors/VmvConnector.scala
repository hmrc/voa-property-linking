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

package connectors

import config.Wiring
import controllers.PropertyDetailsController._
import models.{APIValuationHistory, Property, PropertyAddress}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.HeaderCarrier

object VmvConnector extends ServicesConfig {

  val http = Wiring().http

  def getPropertyInfo(uarn: Long)(implicit hc: HeaderCarrier) = {
    http.GET[JsValue](s"${baseUrl("vmv")}/vmv/rating-listing/api/get-draft-valuation/$uarn") map { r =>
      propertyReads(uarn).reads(r) match {
        case JsSuccess(v, _) => Some(v)
        case JsError(errs) => None
      }
    }
  }

  def getValuationHistory(uarn: Long)(implicit hc: HeaderCarrier) = {
    http.GET[JsValue](s"${baseUrl("external-business-rates-data-platform")}/ndrlist/valuation_history/$uarn").map(js =>{
      (js \ "NDRListValuationHistoryItems").as[Seq[APIValuationHistory]]
    } )
  }

  private def propertyReads(uarn: Long) = (
    (__ \ "billingAuthority" \ "reference").read[String] and
      (__ \ "address" \ "lines" \ "extractedLines").read[Seq[String]] and
      (__ \ "address" \ "postcode" \ "value").read[String] and
      (__ \ "description").read[String] and
      (__ \ "specialCategoryCode").read[String]
    ) (
    (baRef, lines, postcode, desc, scat) =>
      Property(uarn, baRef, PropertyAddress(lines, postcode), isSelfCertifiable(uarn), scat, desc, "BCI")
  )

  private def isSelfCertifiable(uarn: Long) = uarn % 2 == 0 //TODO until business logic is finalised
}
