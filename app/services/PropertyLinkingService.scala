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

package services

import binders.propertylinks.{GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import cats.data.OptionT
import connectors.authorisationsearch.PropertyLinkingConnector
import connectors.externalpropertylink.ExternalPropertyLinkConnector
import connectors.externalvaluation.ExternalValuationManagementApi
import javax.inject.Inject
import models._
import models.mdtp.propertylink.myclients.PropertyLinksWithClients
import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.searchApi.OwnerAuthResult
import models.voa.propertylinking.requests.CreatePropertyLink
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.Cats

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingService @Inject()(
                                        val propertyLinksConnector: ExternalPropertyLinkConnector,
                                        val externalValuationManagementApi: ExternalValuationManagementApi,
                                        val legacyPropertyLinksConnector: PropertyLinkingConnector
                                      ) (implicit executionContext: ExecutionContext) extends Cats {


  def create(propertyLink: APIPropertyLinkRequest)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): Future[HttpResponse] = {
      val createPropertyLink = CreatePropertyLink(propertyLink)
      propertyLinksConnector.createPropertyLink(createPropertyLink)
  }

  def getClientsPropertyLink(submissionId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): OptionT[Future, PropertiesView] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getClientsPropertyLink(submissionId))
    } yield PropertiesView(propertyLink.authorisation, Nil)
  }

  def getMyOrganisationsPropertyLink(submissionId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): OptionT[Future, PropertiesView] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
    } yield PropertiesView(propertyLink.authorisation, Nil)
  }

  def getClientsPropertyLinks(
                               searchParams: GetMyClientsPropertyLinkParameters,
                               paginationParams: Option[PaginationParams]
                             )(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]):OptionT[Future, PropertyLinksWithClients] = {
    OptionT(propertyLinksConnector.getClientsPropertyLinks(searchParams, paginationParams)).map(PropertyLinksWithClients.apply)
  }

  def getMyOrganisationsPropertyLinks(searchParams: GetMyOrganisationPropertyLinksParameters, paginationParams: Option[PaginationParams])
                                     (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]):OptionT[Future, OwnerAuthResult] = {
    OptionT(propertyLinksConnector.getMyOrganisationsPropertyLinks(searchParams, paginationParams)).map(OwnerAuthResult.apply)
  }
}
