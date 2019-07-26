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

import binders.GetPropertyLinksParameters
import cats.data.OptionT
import connectors.{ExternalPropertyLinkConnector, ExternalValuationManagementApi, PropertyLinkingConnector}
import javax.inject.Inject
import models._
import models.mdtp.propertylinking.requests.APIPropertyLinkRequest
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import models.searchApi.OwnerAuthResult
import models.voa.propertylinking.requests.CreatePropertyLink
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
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield PropertiesView(propertyLink.authorisation, history.NDRListValuationHistoryItems)
  }

  def getMyOrganisationsPropertyLink(submissionId: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]): OptionT[Future, PropertiesView] = {
    for {
      propertyLink <- OptionT(propertyLinksConnector.getMyOrganisationsPropertyLink(submissionId))
      history  <- OptionT(externalValuationManagementApi.getValuationHistory(propertyLink.authorisation.uarn, submissionId))
    } yield PropertiesView(propertyLink.authorisation, history.NDRListValuationHistoryItems)
  }

  def getClientsPropertyLinks( searchParams: GetPropertyLinksParameters, paginationParams: Option[PaginationParams])
                             (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]):OptionT[Future, OwnerAuthResult] = {
    OptionT(propertyLinksConnector.getClientsPropertyLinks(searchParams, paginationParams)).map(OwnerAuthResult.apply)
  }

  def getMyOrganisationsPropertyLinks( searchParams: GetPropertyLinksParameters, paginationParams: Option[PaginationParams])
                                     (implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_]):OptionT[Future, OwnerAuthResult] = {
    OptionT(propertyLinksConnector.getMyOrganisationsPropertyLinks(searchParams, paginationParams)).map(OwnerAuthResult.apply)
  }
}
