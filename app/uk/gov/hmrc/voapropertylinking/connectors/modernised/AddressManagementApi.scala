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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import javax.inject.Inject
import models.{APIAddressLookupResult, DetailedAddress, SimpleAddress}
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AddressManagementApi @Inject()(
                                      http: DefaultHttpClient,
                                      servicesConfig: ServicesConfig
                                    )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform")
  lazy val url = baseUrl + "/address-management-api/address"

  def find(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[DetailedAddress]] = {
    http.GET[APIAddressLookupResult](s"""$url?pageSize=100&startPoint=1&searchparams={"postcode": "$postcode"}""") map { res =>
      res.addressDetails
    }
  }

  def get(addressUnitId: Long)(implicit hc: HeaderCarrier): Future[Option[SimpleAddress]] = {
    http.GET[APIAddressLookupResult](s"$url/$addressUnitId").map(_.addressDetails.headOption.map(_.simplify)).recover {
      case _ => None
    }
  }

  def create(address: SimpleAddress)(implicit hc: HeaderCarrier): Future[Long] = {
    http.POST[DetailedAddress, JsValue](s"$url/non_standard_address", address.toDetailedAddress) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(n)) => n.toLong
        case _ => throw new Exception(s"Failed to create record for address $address")
      }
    }
  }

  def update(nonAbpId: Long, address: DetailedAddress)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[DetailedAddress, HttpResponse](s"$url/non_standard_address/$nonAbpId", address) map { _ => () }
  }

}
