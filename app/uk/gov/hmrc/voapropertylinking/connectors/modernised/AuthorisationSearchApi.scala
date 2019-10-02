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
import models.searchApi.OwnerAuthResult
import models.searchApi.{AgentAuthResultBE, Agents}
import models.{PaginationParams, PropertyRepresentations}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationSearchApi @Inject()(
                                        http: DefaultHttpClient,
                                        servicesConfig: ServicesConfig
                                      )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform")

  def searchAndSort(organisationId: Long,
                    params: PaginationParams,
                    sortfield: Option[String] = None,
                    sortorder: Option[String] = None,
                    status: Option[String] = None,
                    address: Option[String] = None,
                    baref: Option[String] = None,
                    agent: Option[String] = None,
                    agentAppointed: Option[String] = Some("BOTH"))(implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {
    val url = baseUrl +
      s"/authorisation-search-api/owners/$organisationId/authorisations" +
      s"?start=${params.startPoint}" +
      s"&size=${params.pageSize}" +
      buildQueryParams("sortfield", sortfield) +
      buildQueryParams("sortorder", sortorder) +
      buildQueryParams("status", status) +
      buildQueryParams("address", address) +
      buildQueryParams("baref", baref) +
      buildQueryParams("agent", agent) +
      s"&agentAppointed=${agentAppointed.getOrElse("BOTH")}"

    http.GET[OwnerAuthResult](url).map(_.uppercase)
  }

  def appointableToAgent(
                          ownerId: Long,
                          agentId: Long,
                          checkPermission: Option[String],
                          challengePermission: Option[String],
                          params: PaginationParams,
                          sortfield: Option[String] = None,
                          sortorder: Option[String] = None,
                          address: Option[String] = None,
                          agent: Option[String] = None)(implicit hc: HeaderCarrier): Future[OwnerAuthResult] = {
    val url = baseUrl +
      s"/authorisation-search-api/owners/$ownerId/agents/$agentId/availableAuthorisations" +
      s"?start=${params.startPoint}&size=${params.pageSize}" +
      buildQueryParams("check", checkPermission) +
      buildQueryParams("challenge", challengePermission) +
      buildQueryParams("sortfield", sortfield) +
      buildQueryParams("sortorder", sortorder) +
      buildQueryParams("address", address) +
      buildQueryParams("agent", agent)

    http.GET[OwnerAuthResult](url).map(_.uppercase)
  }

  def forAgent(status: String, organisationId: Long, params: PaginationParams)(implicit hc: HeaderCarrier): Future[PropertyRepresentations] = {
    val url = s"$baseUrl/authorisation-search-api/agents/$organisationId/authorisations"

    http.GET[AgentAuthResultBE](url, Seq("start" -> params.startPoint.toString, "size" -> params.pageSize.toString, "representationStatus" -> "PENDING")).map(x => {
      PropertyRepresentations(x.filterTotal, x.authorisations.map(_.toPropertyRepresentation))
    })
  }

  def manageAgents (organisationId: Long)(implicit hc: HeaderCarrier): Future[Agents] = {
    val url = baseUrl +
      s"/authorisation-search-api/owners/$organisationId/agents"
    http.GET[Agents](url)
  }

  private def buildQueryParams(name: String, value: Option[String]): String = {
    value match {
      case Some(paramValue) if paramValue != "" => s"&$name=$paramValue";
      case _ => ""
    }
  }

}
