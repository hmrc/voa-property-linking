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

import models.modernised.addressmanagement.{Addresses, DetailedAddress, SimpleAddress}
import play.api.libs.json._
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ModernisedAddressManagementApi @Inject() (
      httpClient: VoaHttpClient,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  private val url = s"${appConfig.modernisedBase}/address-management-api/address"

  def find(postcode: String)(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Seq[DetailedAddress]] = {
    val encodedParams = URLEncoder.encode(s"""{"postcode":"$postcode"}""", StandardCharsets.UTF_8.toString)
    val fullUrl = s"$url?pageSize=100&startPoint=1&searchparams=$encodedParams"
    httpClient
      .getWithGGHeaders[Addresses](fullUrl)
      .map(_.addressDetails)
  }

  def get(addressUnitId: Long)(implicit request: RequestWithPrincipal[_]): Future[Option[SimpleAddress]] =
    httpClient
      .getWithGGHeaders[Addresses](s"$url/$addressUnitId")
      .map(_.addressDetails.headOption.map(_.simplify)) recover toNone

  def create(address: SimpleAddress)(implicit request: RequestWithPrincipal[_]): Future[Long] =
    httpClient
      .postWithGgHeaders[JsValue](
        s"$url/non_standard_address",
        Json.toJsObject(address.toDetailedAddress)
      )
      .map { json =>
        (json \ "id").asOpt[Long].getOrElse {
          throw new Exception(s"Failed to create record for address $address: Missing or invalid 'id'")
        }
      }
}
