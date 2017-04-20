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
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector @Inject() (@Named("VoaBackendWsHttp") http: WSHttp)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")
  val listYear = 2017

  def create(linkingRequest: APIPropertyLinkRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-management-api/property/save_property_link"
    http.POST[APIPropertyLinkRequest, HttpResponse](url, linkingRequest) map { _ => () }
  }

  def find(organisationId: Long, params: PaginationParams)(implicit hc: HeaderCarrier): Future[PropertiesView] = {
    val url = baseUrl +
      s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view" +
      s"?listYear=$listYear" +
      s"&organisationId=$organisationId" +
      s"&startPoint=${params.startPoint}" +
      s"&pageSize=${params.pageSize}" +
      s"&requestTotalRowCount=${params.requestTotalRowCount}"

    http.GET[PropertiesView](url)
      .map(filterInvalid)
      .map(filterParties)

  }

  private def filterInvalid(view: PropertiesView): PropertiesView = {
    view.copy(authorisations = view.authorisations.filterNot(a => Seq("REVOKED", "DECLINED").contains(a.authorisationStatus.toUpperCase)))
  }

  private def filterParties(view: PropertiesView): PropertiesView = {
    val filtered = view.authorisations map { auth =>
      val filteredParties = auth.parties
        .filter(party => List("APPROVED", "PENDING").contains(party.authorisedPartyStatus)) //parties must be approved or pending
        .map(party => party.copy(permissions =  party.permissions.filterNot(_.endDate.isDefined))) //permissions can't have enddate
        .filter(_.permissions.nonEmpty) //and agent must have a permission

      auth.copy(parties = filteredParties)
    }

    view.copy(authorisations = filtered)
  }

  def getAssessment(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[Assessment]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment?listYear=$listYear&authorisationId=$authorisationId"
    http.GET[APIAuthorisation](url).map(pLink => {
      pLink.NDRListValuationHistoryItems.map(assessment => Assessment.fromAPIValuationHistory(assessment, authorisationId, CapacityDeclaration(pLink.authorisationOwnerCapacity, pLink.startDate, pLink.endDate)))
    })
  }
}
