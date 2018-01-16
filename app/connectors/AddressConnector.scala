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

package connectors

import javax.inject.{Inject, Named}

import models.{APIAddressLookupResult, DetailedAddress, SimpleAddress}
import play.api.libs.json.{JsDefined, JsNumber, JsValue}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class AddressConnector @Inject() (@Named("VoaBackendWsHttp") http: WSHttp, conf: ServicesConfig)(implicit ec: ExecutionContext) {

  val url = conf.baseUrl("external-business-rates-data-platform") + "/address-management-api/address"

  def find(postcode: String)(implicit hc: HeaderCarrier): Future[Seq[SimpleAddress]] = {
    http.GET[APIAddressLookupResult](s"""$url?pageSize=100&startPoint=1&searchparams={"postcode": "$postcode"}""") map { res =>
      res.addressDetails.map(_.simplify)
    }
  }

  def get(addressUnitId: Int)(implicit hc: HeaderCarrier): Future[Option[SimpleAddress]] = {
    http.GET[APIAddressLookupResult](s"$url/$addressUnitId").map(_.addressDetails.headOption.map(_.simplify)).recover {
      case _ => None
    }
  }

  def create(address: SimpleAddress)(implicit hc: HeaderCarrier): Future[Int] = {
    http.POST[DetailedAddress, JsValue](s"$url/non_standard_address", address.toDetailedAddress) map { js =>
      js \ "id" match {
        case JsDefined(JsNumber(n)) => n.toInt
        case _ => throw new Exception(s"Failed to create record for address $address")
      }
    }
  }

  def update(nonAbpId: Int, address: DetailedAddress)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[DetailedAddress, HttpResponse](s"$url/non_standard_address/$nonAbpId", address) map { _ => () }
  }
}
