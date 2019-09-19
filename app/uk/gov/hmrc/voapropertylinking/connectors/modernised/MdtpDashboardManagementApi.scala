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
import models.{APIParty, PropertiesView}
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class MdtpDashboardManagementApi @Inject()(
                                            http: DefaultHttpClient,
                                            servicesConfig: ServicesConfig
                                          )(implicit executionContext: ExecutionContext) extends BaseVoaConnector {

  lazy val baseUrl: String = servicesConfig.baseUrl("external-business-rates-data-platform")

  val listYear = 2017

  //TODO remove this as it is not an authed endpoint
  def get(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertiesView]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment"

    http.GET[Option[PropertiesView]](url, Seq("listYear" -> s"$listYear", "authorisationId" -> s"$authorisationId")) map {
      case Some(view) if view.hasValidStatus => Some(view.copy(parties = filterInvalidParties(view.parties)).upperCase)
      case _ => None
    }
  }

  def getAssessment(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Option[PropertiesView]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment?listYear=$listYear&authorisationId=$authorisationId"
    http.GET[Option[PropertiesView]](url).map(_.map(_.upperCase)) recover { case _: NotFoundException => None }
  }

  private val withValidPermissions: APIParty => Boolean = { party =>
    List("APPROVED", "PENDING").contains(party.authorisedPartyStatus) && party.permissions.exists(_.endDate.isEmpty)
  }

  private val filterInvalidParties: Seq[APIParty] => Seq[APIParty] = { parties =>
    parties.withFilter(withValidPermissions).map(p => p.copy(permissions = p.permissions.filter(_.endDate.isEmpty)))
  }
}
