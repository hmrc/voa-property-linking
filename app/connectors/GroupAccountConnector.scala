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

import java.time.Clock
import javax.inject.Inject

import com.google.inject.name.Named
import models._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class GroupAccountConnector @Inject()(@Named("VoaBackendWsHttp") http: WSHttp, conf: ServicesConfig)(implicit ec: ExecutionContext, clock: Clock) {

  lazy val baseUrl: String = conf.baseUrl("external-business-rates-data-platform")
  lazy val url = baseUrl + "/customer-management-api/organisation"

  def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[GroupId] = {
    http.POST[APIGroupAccountSubmission, GroupId](url, account.toApiAccount)
  }

  def update(orgId: Long, account: UpdatedOrganisationAccount)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.PUT[UpdatedOrganisationAccount, HttpResponse](s"$url/$orgId", account) map { _ => () }
  }

  def get(id: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[APIDetailedGroupAccount]](s"$url?organisationId=$id") map { _.map { _.toGroupAccount } }
  }

  def findByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[APIDetailedGroupAccount]](s"$url?governmentGatewayGroupId=$ggId") map { _.map { _.toGroupAccount } }
  }

  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[APIDetailedGroupAccount]](s"$url?representativeCode=$agentCode") map { _.map { _.toGroupAccount } }
  }
}