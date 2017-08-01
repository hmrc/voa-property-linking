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

import models._
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector @Inject() (@Named("VoaBackendWsHttp") http: WSHttp, conf: ServicesConfig)(implicit ec: ExecutionContext) {
  lazy val baseUrl: String = conf.baseUrl("external-business-rates-data-platform")
  val listYear = 2017

  def create(linkingRequest: APIPropertyLinkRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-management-api/property/save_property_link"
    http.POST[APIPropertyLinkRequest, HttpResponse](url, linkingRequest) map { _ => () }
  }

  def get(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertiesView]] = {
    val url = baseUrl +
      s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment" +
      s"?listYear=$listYear" +
      s"&authorisationId=$authorisationId"

    http.GET[Option[PropertiesView]](url) map {
      case Some(view) if view.hasValidStatus => Some(view.copy(parties = filterInvalidParties(view.parties)))
      case _ => None
    } recover {
      case _: NotFoundException => None
    }
  }

  def find(organisationId: Long, params: PaginationParams)(implicit hc: HeaderCarrier): Future[PropertiesViewResponse] = {
    val url = baseUrl +
      s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
      s"?listYear=$listYear" +
      s"&organisationId=$organisationId" +
      s"&startPoint=${params.startPoint}" +
      s"&pageSize=${params.pageSize}" +
      s"&requestTotalRowCount=${params.requestTotalRowCount}"

    http.GET[PropertiesViewResponse](url).map(withValidStatuses.andThen(withValidParties))
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

  private val withValidParties: PropertiesViewResponse => PropertiesViewResponse = { view =>
    view.copy(authorisations = view.authorisations map { auth => auth.copy(parties = filterInvalidParties(auth.parties))})
  }

  def getAssessment(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertiesView]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment?listYear=$listYear&authorisationId=$authorisationId"
    http.GET[Option[PropertiesView]](url) recover { case _: NotFoundException => None }
  }
}
