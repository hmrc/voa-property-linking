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
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.voapropertylinking.auth.RequestWithPrincipal
import uk.gov.hmrc.voapropertylinking.config.AppConfig
import uk.gov.hmrc.voapropertylinking.connectors.BaseVoaConnector
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.ModernisedRequestErrorLogging
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ModernisedCustomerManagementApi @Inject() (
      httpClient: VoaHttpClient,
      appConfig: AppConfig
)(implicit executionContext: ExecutionContext)
    extends BaseVoaConnector with ModernisedRequestErrorLogging {

  lazy val baseUrl: String = s"${appConfig.modernisedBase}/customer-management-api"
  lazy val organisationUrl: String = baseUrl + "/organisation"
  lazy val individualUrl: String = baseUrl + "/person"

  def createGroupAccount(account: GroupAccountSubmission, time: Instant = Instant.now)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[GroupId] = {
    val response = postJsonWithGGHeaders[GroupId](httpClient, organisationUrl, Json.toJsObject(account.toApiAccount(time)))
    logModernisedErrorResponse(response, Seq.empty, organisationUrl)(requestWithPrincipal.principal, executionContext)
  }

  def updateGroupAccount(orgId: Long, account: UpdatedOrganisationAccount)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[Unit] = {
    val orgUrl = s"$organisationUrl/$orgId"
    val response = putRawWithGGHeaders(httpClient, orgUrl, Json.toJsObject(account)).map { _ =>
      ()
    }
    logModernisedErrorResponse(response, Seq("orgId" -> orgId.toString), orgUrl)(
      requestWithPrincipal.principal,
      executionContext
    )
  }

  def getDetailedGroupAccount(
        id: Long
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] = {
    val url = s"$organisationUrl?organisationId=$id"
    val response = getOptionalJsonWithGGHeaders[APIDetailedGroupAccount](httpClient, url)
      .map(_.map(_.toGroupAccount))
    logModernisedErrorResponse(response, Seq("organisationId" -> id.toString), url)(
      requestWithPrincipal.principal,
      executionContext
    )
  }

  def findDetailedGroupAccountByGGID(
        ggId: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] = {
    val url = s"$organisationUrl?governmentGatewayGroupId=$ggId"
    val response = getOptionalJsonWithGGHeaders[APIDetailedGroupAccount](httpClient, url)
      .map(_.map(_.toGroupAccount))
    logModernisedErrorResponse(response, Seq("ggId" -> ggId), url)(requestWithPrincipal.principal, executionContext)
  }

  def withAgentCode(
        agentCode: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[GroupAccount]] = {
    val url = s"$organisationUrl?representativeCode=$agentCode"
    val response = getOptionalJsonWithGGHeaders[APIDetailedGroupAccount](httpClient, url)
      .map(_.map(_.toGroupAccount))
    logModernisedErrorResponse(response, Seq("agentCode" -> agentCode), url)(
      requestWithPrincipal.principal,
      executionContext
    )
  }

  def createIndividualAccount(account: IndividualAccountSubmission, time: Instant = Instant.now)(implicit
        requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[IndividualAccountId] = {
    val response = postJsonWithGGHeaders[IndividualAccountId](
      httpClient,
      individualUrl,
      Json.toJsObject(account.toAPIIndividualAccount(time))
    )
    logModernisedErrorResponse(response, Seq.empty, individualUrl)(requestWithPrincipal.principal, executionContext)
  }

  def updateIndividualAccount(personId: Long, account: IndividualAccountSubmission, time: Instant = Instant.now)(
        implicit requestWithPrincipal: RequestWithPrincipal[_]
  ): Future[JsValue] = {
    val personUrl = individualUrl + s"/$personId"
    val response = putJsonWithGGHeaders[JsValue](
      httpClient,
      personUrl,
      Json.toJsObject(account.toAPIIndividualAccount(time))
    )
    logModernisedErrorResponse(response, Seq("personId" -> personId.toString), personUrl)(
      requestWithPrincipal.principal,
      executionContext
    )
  }

  def getDetailedIndividual(
        id: Long
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[IndividualAccount]] = {
    val url = s"$individualUrl?personId=$id"
    val response = getOptionalJsonWithGGHeaders[APIDetailedIndividualAccount](httpClient, url)
      .map(_.map(a => a.toIndividualAccount))
    logModernisedErrorResponse(response, Seq("personId" -> id.toString), url)(
      requestWithPrincipal.principal,
      executionContext
    )
  }

  def findDetailedIndividualAccountByGGID(
        ggId: String
  )(implicit requestWithPrincipal: RequestWithPrincipal[_]): Future[Option[IndividualAccount]] = {
    val url = s"$individualUrl?governmentGatewayExternalId=$ggId"
    val response = getOptionalJsonWithGGHeaders[APIDetailedIndividualAccount](httpClient, url)
      .map(_.map(_.toIndividualAccount))
    logModernisedErrorResponse(response, Seq("ggId" -> ggId), url)(requestWithPrincipal.principal, executionContext)
  }
}
