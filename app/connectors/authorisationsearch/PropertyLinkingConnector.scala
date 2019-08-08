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

package connectors.authorisationsearch

import javax.inject.{Inject, Named}
import models._
import models.modernised.mdtpdashboard.LegacyPropertiesView
import models.searchApi.OwnerAuthResult
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector @Inject()(
                                          @Named("VoaBackendWsHttp") http: WSHttp,
                                          conf: ServicesConfig
                                        )(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = conf.baseUrl("external-business-rates-data-platform")
  val listYear = 2017

  //TODO remove this as it is not an authed endpoint
  def get(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[LegacyPropertiesView]] = {
    val url = baseUrl +
      s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
      s"?listYear=$listYear" +
      s"&authorisationId=$authorisationId"

    http.GET[Option[LegacyPropertiesView]](url) map {
      case Some(view) if view.hasValidStatus => Some(view.copy(parties = filterInvalidParties(view.parties)).upperCase)
      case _ => None
    }
  }

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

  def getCapacity(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[Capacity]] = {
    val url = baseUrl + s"/authorisation-management-api/authorisation/$authorisationId"

    http.GET[Option[Capacity]](url)
  }

  private def buildQueryParams(name: String, value: Option[String]): String = {
    value match {
      case Some(paramValue) if paramValue != "" => s"&$name=$paramValue";
      case _ => ""
    }
  }

  private val withValidStatuses: PropertiesViewResponse => PropertiesViewResponse = { view =>
    view.copy(authorisations = view.authorisations.filter(_.hasValidStatus))
  }

  private val withValidPermissions: APIParty => Boolean = { party =>
    List("APPROVED", "PENDING").contains(party.authorisedPartyStatus) && party.permissions.exists(_.endDate.isEmpty)
  }

  private val filterInvalidParties: Seq[APIParty] => Seq[APIParty] = { parties =>
    parties.withFilter(withValidPermissions).map(p => p.copy(permissions = p.permissions.filter(_.endDate.isEmpty)))
  }

  def getAssessment(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertiesView]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment?listYear=$listYear&authorisationId=$authorisationId"
    http.GET[Option[PropertiesView]](url).map(_.map(_.upperCase)) recover { case _: NotFoundException => None }
  }
}
