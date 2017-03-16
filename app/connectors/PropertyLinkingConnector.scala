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

import java.net.URLEncoder
import javax.inject.Inject

import config.VOABackendWSHttp
import models._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector @Inject() (http: VOABackendWSHttp)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")
  val listYear = 2017

  private def liveAuthorisation(a: APIAuthorisation) = !Seq("REVOKED", "DECLINED").contains(a.authorisationStatus.toUpperCase)


  def get(authorisationId: Long)(implicit hc: HeaderCarrier): Future[APIAuthorisation] = {
    val url = s"$baseUrl/authorisation-management-api/authorisation/$authorisationId"
    http.GET[APIAuthorisation](url)
  }

  def create(linkingRequest: APIPropertyLinkRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-management-api/property/save_property_link"
    http.POST[APIPropertyLinkRequest, HttpResponse](url, linkingRequest) map { _ => () }
  }

  def setEnd(authorisationId: Long, endRequest: APIPropertyLinkEndDateRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/authorisation-management-api/authorisation/${authorisationId}"
    http.PATCH[APIPropertyLinkEndDateRequest, HttpResponse](url, endRequest) map { _ => () }
  }

  def find(organisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[APIAuthorisation]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/properties_view?listYear=$listYear&organisationId=$organisationId"
    val props = http.GET[JsValue](url).map(js =>{
      (js \ "authorisations").as[Seq[APIAuthorisation]]
    }).map(_.filter(liveAuthorisation(_))
    )
    props.map(_.map(x=> {
      x.copy(parties = {
        x.parties
          .filter(party => List("APPROVED", "PENDING").contains(party.authorisedPartyStatus)) //parties must be approved or pending
          .map(party => party.copy(permissions =  party.permissions.filterNot(_.endDate.isDefined))) //permissions can't have enddate
          .filter(_.permissions.nonEmpty) //and agent must have a permission
      })
    }))
  }

  def findFor(organisationId: Long, uarn: Long)(implicit hc: HeaderCarrier): Future[Seq[APIAuthorisation]] = {
    val searchParameters = new APIAuthorisationQuery(organisationId, uarn)
    val url = baseUrl + s"/authorisation-management-api/authorisation?startPoint=1&pageSize=100&searchParameters=${URLEncoder.encode(Json.toJson(searchParameters).toString, "UTF-8")}"
    http.GET[JsValue](url).map(js => {
      (js \ "authorisations").as[Seq[APIAuthorisation]]
    }).map(_.filter(liveAuthorisation(_))
    )
  }


  def getAssessment(authorisationId: Long)(implicit hc: HeaderCarrier): Future[Seq[Assessment]] = {
    val url = baseUrl + s"/mdtp-dashboard-management-api/mdtp_dashboard/view_assessment?listYear=$listYear&authorisationId=$authorisationId"
    http.GET[APIAuthorisation](url).map(pLink => {
      pLink.NDRListValuationHistoryItems.map(assessment => Assessment.fromAPIValuationHistory(assessment, authorisationId, CapacityDeclaration(pLink.authorisationOwnerCapacity, pLink.startDate, pLink.endDate)))
    })
  }
}
