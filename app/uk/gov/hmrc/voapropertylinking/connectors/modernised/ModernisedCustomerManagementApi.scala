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

import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ModernisedCustomerManagementApi @Inject()(
      http: DefaultHttpClient,
      servicesConfig: ServicesConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("voa-modernised-api") + "/customer-management-api"
  lazy val organisationUrl: String = baseUrl + "/organisation"
  lazy val individualUrl: String = baseUrl + "/person"

  def createGroupAccount(account: GroupAccountSubmission, time: Instant = Instant.now)(
        implicit hc: HeaderCarrier): Future[GroupId] =
    http.POST[APIGroupAccountSubmission, GroupId](organisationUrl, account.toApiAccount(time))

  def updateGroupAccount(orgId: Long, account: UpdatedOrganisationAccount)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[UpdatedOrganisationAccount, HttpResponse](s"$organisationUrl/$orgId", account) map { _ =>
      ()
    }

  //should never return none
  def getDetailedGroupAccount(id: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http.GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?organisationId=$id").map(_.map(_.toGroupAccount))

  //should never return none
  def findDetailedGroupAccountByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http
      .GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?governmentGatewayGroupId=$ggId")
      .map(_.map(_.toGroupAccount))

  //should never return none
  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http
      .GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?representativeCode=$agentCode")
      .map(_.map(_.toGroupAccount))

  def createIndividualAccount(account: IndividualAccountSubmission, time: Instant = Instant.now)(
        implicit hc: HeaderCarrier): Future[IndividualAccountId] =
    http.POST[APIIndividualAccount, IndividualAccountId](individualUrl, account.toAPIIndividualAccount(time))

  def updateIndividualAccount(personId: Long, account: IndividualAccountSubmission, time: Instant = Instant.now)(
        implicit hc: HeaderCarrier): Future[JsValue] =
    http.PUT[APIIndividualAccount, JsValue](individualUrl + s"/$personId", account.toAPIIndividualAccount(time))

  def getDetailedIndividual(id: Long)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] =
    http
      .GET[Option[APIDetailedIndividualAccount]](s"$individualUrl?personId=$id")
      .map(_.map(a => a.toIndividualAccount))

  def findDetailedIndividualAccountByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] =
    http
      .GET[Option[APIDetailedIndividualAccount]](s"$individualUrl?governmentGatewayExternalId=$ggId")
      .map(_.map(_.toIndividualAccount))
}
