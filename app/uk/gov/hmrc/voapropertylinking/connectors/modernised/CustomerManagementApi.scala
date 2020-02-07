/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Named}
import models._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.voapropertylinking.models.modernoised.agentrepresentation.AgentOrganisation

import scala.concurrent.{ExecutionContext, Future}

class CustomerManagementApi @Inject()(
                                       http: DefaultHttpClient,
                                       servicesConfig: ServicesConfig,
                                       @Named("voa.agentByRepresentationCode") agentByRepresentationCodeUrl: String
                                     )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform") + "/customer-management-api"
  lazy val organisationUrl: String = baseUrl + "/organisation"
  lazy val individualUrl: String = baseUrl + "/person"

  lazy val voaModernisedApiStubBaseUrl: String = servicesConfig.baseUrl("voa-modernised-api")

  def createGroupAccount(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[GroupId] =
    http.POST[APIGroupAccountSubmission, GroupId](organisationUrl, account.toApiAccount)

  def updateGroupAccount(orgId: Long, account: UpdatedOrganisationAccount)(implicit hc: HeaderCarrier): Future[Unit] =
    http.PUT[UpdatedOrganisationAccount, HttpResponse](s"$organisationUrl/$orgId", account) map { _ => () }

  def getDetailedGroupAccount(id: Long)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http.GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?organisationId=$id").map(_.map(_.toGroupAccount))

  def findDetailedGroupAccountByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http.GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?governmentGatewayGroupId=$ggId").map(_.map(_.toGroupAccount))

  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] =
    http.GET[Option[APIDetailedGroupAccount]](s"$organisationUrl?representativeCode=$agentCode").map(_.map(_.toGroupAccount))

  def createIndividualAccount(account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[IndividualAccountId] =
    http.POST[APIIndividualAccount, IndividualAccountId](individualUrl, account.toAPIIndividualAccount)

  def updateIndividualAccount(personId: Long, account: IndividualAccountSubmission)(implicit hc: HeaderCarrier): Future[JsValue] =
    http.PUT[APIIndividualAccount, JsValue](individualUrl + s"/$personId", account.toAPIIndividualAccount)

  def getDetailedIndividual(id: Long)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] =
    http.GET[Option[APIDetailedIndividualAccount]](s"$individualUrl?personId=$id").map(_.map(a => a.toIndividualAccount))

  def findDetailedIndividualAccountByGGID(ggId: String)(implicit hc: HeaderCarrier): Future[Option[IndividualAccount]] =
    http.GET[Option[APIDetailedIndividualAccount]](s"$individualUrl?governmentGatewayExternalId=$ggId").map(_.map(_.toIndividualAccount))

  def getAgentByRepresentationCode(agentCode: Long)(implicit hc: HeaderCarrier): Future[Option[AgentOrganisation]] = {
    http.GET[Option[AgentOrganisation]](agentByRepresentationCodeUrl.templated("agentCode" -> agentCode)) recover {
      case _: NotFoundException => {
        None
      }
    }
  }

}
