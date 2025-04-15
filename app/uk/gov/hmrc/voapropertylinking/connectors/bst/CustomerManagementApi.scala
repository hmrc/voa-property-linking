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

package uk.gov.hmrc.voapropertylinking.connectors.bst

import models._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CustomerManagementApi @Inject() (
      httpClient: VoaHttpClient,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector {

  lazy val baseUrl: String = s"${appConfig.bstBase}/customer-management-api"
  lazy val organisationUrl: String = baseUrl + "/organisation"
  lazy val individualUrl: String = baseUrl + "/person"

  def createGroupAccount(account: GroupAccountSubmission, time: Instant = Instant.now)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[GroupId] =
    httpClient.postWithGgHeaders[GroupId](organisationUrl, Json.toJsObject(account.toApiAccount(time)))

  def updateGroupAccount(orgId: Long, account: UpdatedOrganisationAccount)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[Unit] =
    httpClient.putWithGgHeaders[HttpResponse](s"$organisationUrl/$orgId", Json.toJsObject(account)).map { _ =>
      ()
    }

  def getDetailedGroupAccount(
        id: Long
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] =
    httpClient
      .getWithGGHeaders[Option[APIDetailedGroupAccount]](s"$organisationUrl?organisationId=$id")
      .map(_.map(_.toGroupAccount))

  def findDetailedGroupAccountByGGID(
        ggId: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] =
    httpClient
      .getWithGGHeaders[Option[APIDetailedGroupAccount]](s"$organisationUrl?governmentGatewayGroupId=$ggId")
      .map(_.map(_.toGroupAccount))

  def withAgentCode(
        agentCode: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] =
    httpClient
      .getWithGGHeaders[Option[APIDetailedGroupAccount]](s"$organisationUrl?representativeCode=$agentCode")
      .map(_.map(_.toGroupAccount))

  def createIndividualAccount(account: IndividualAccountSubmission, time: Instant = Instant.now)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[IndividualAccountId] =
    httpClient
      .postWithGgHeaders[IndividualAccountId](individualUrl, Json.toJsObject(account.toAPIIndividualAccount(time)))

  def updateIndividualAccount(personId: Long, account: IndividualAccountSubmission, time: Instant = Instant.now)(
        implicit requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[JsValue] =
    httpClient
      .putWithGgHeaders[JsValue](individualUrl + s"/$personId", Json.toJsObject(account.toAPIIndividualAccount(time)))

  def getDetailedIndividual(
        id: Long
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[IndividualAccount]] =
    httpClient
      .getWithGGHeaders[Option[APIDetailedIndividualAccount]](s"$individualUrl?personId=$id")
      .map(_.map(a => a.toIndividualAccount))

  def findDetailedIndividualAccountByGGID(
        ggId: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[IndividualAccount]] =
    httpClient
      .getWithGGHeaders[Option[APIDetailedIndividualAccount]](s"$individualUrl?governmentGatewayExternalId=$ggId")
      .map(_.map(_.toIndividualAccount))
}
