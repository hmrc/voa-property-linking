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

object EvidenceType extends Enumeration {
  type EvidenceType = Value

  val RATES_BILL = Value("ratesBill")
  val LEASE = Value("lease")
  val LICENSE = Value("license")
  val SERVICE_CHARGE = Value("serviceCharge")
  val STAMP_DUTY_LAND_TAX_FORM = Value("stampDutyLandTaxForm")
  val WATER_RATE_DEMAND = Value("waterRateDemand")
  val OTHER_UTILITY_BILL = Value("otherUtilityBill")

  implicit val format: Format[EvidenceType] = enumFormat(EvidenceType)

}
