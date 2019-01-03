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

package connectors

import javax.inject.{Inject, Named}
import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class IndividualAccountConnector @Inject()(addresses: AddressConnector,
                                           @Named("VoaBackendWsHttp") http: WSHttp)(implicit ec: ExecutionContext)
  extends ServicesConfig {

  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")
  lazy val url = baseUrl + "/customer-management-api/person"

  def create(account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[IndividualAccountId] = {
    http.POST[APIIndividualAccount, IndividualAccountId](url, account.toAPIIndividualAccount)
  }

  def update(personId: Long, account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[JsValue] = {
    http.PUT[APIIndividualAccount, JsValue](url + s"/$personId", account.toAPIIndividualAccount)
  }

  def get(id: Long)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = {
    http.GET[Option[APIDetailedIndividualAccount]](s"$url?personId=$id") map {
      _.map(a => a.toIndividualAccount)
    }
  }

  def findByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = {
    http.GET[Option[APIDetailedIndividualAccount]](s"$url?governmentGatewayExternalId=$ggId") map {
      _.map(_.toIndividualAccount)
    }
  }

}
