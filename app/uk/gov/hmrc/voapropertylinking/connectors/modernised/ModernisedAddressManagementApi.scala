/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import javax.inject.Inject
import models.modernised.addressmanagement.{Addresses, DetailedAddress, SimpleAddress}
import play.api.libs.json.{JsDefined, JsNumber, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector

import scala.concurrent.{ExecutionContext, Future}

class ModernisedAddressManagementApi @Inject()(
      http: HttpClient,
      servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val url: String = servicesConfig.baseUrl("voa-modernised-api") + "/address-management-api/address"

  def find(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[DetailedAddress]] =
    http
      .GET[Addresses](s"""$url?pageSize=100&startPoint=1&searchparams={"postcode": "$postcode"}""")
      .map(_.addressDetails)

  def get(addressUnitId: Long)(implicit hc: HeaderCarrier): Future[Option[SimpleAddress]] =
    http.GET[Addresses](s"$url/$addressUnitId").map(_.addressDetails.headOption.map(_.simplify)) recover toNone

  def create(address: SimpleAddress)(implicit hc: HeaderCarrier): Future[Long] =
    http.POST[DetailedAddress, JsValue](s"$url/non_standard_address", address.toDetailedAddress) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(n)) => n.toLong
        case _                      => throw new Exception(s"Failed to create record for address $address")
      }
    }
}
