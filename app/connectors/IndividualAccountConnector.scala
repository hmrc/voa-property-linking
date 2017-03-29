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

import javax.inject.{Inject, Named}

import com.google.inject.Singleton
import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IndividualAccountConnector @Inject()(addresses: AddressConnector,
                                           @Named("VoaBackendWsHttp") http: WSHttp)(implicit ec: ExecutionContext)
  extends ServicesConfig {

  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform") + "/customer-management-api/person"

  def create(account: IndividualAccountWrite)(implicit hc: HeaderCarrier): Future[JsValue] = {
    http.POST[APIIndividualAccount, JsValue](baseUrl, account.toAPIIndividualAccount)
  }

  def get(id: Int)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = {
    http.GET[Option[APIDetailedIndividualAccount]](s"$baseUrl?personId=$id") map {
      _.map(a => a.toIndividualAccount)
    }
  }

  def findByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] = {
    http.GET[Option[APIDetailedIndividualAccount]](s"$baseUrl?governmentGatewayExternalId=$ggId") map {
      _.map(_.toIndividualAccount)
    }
  }

}
