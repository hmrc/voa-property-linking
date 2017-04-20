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

package controllers

import javax.inject.Inject

import connectors.{GroupAccountConnector, PropertyLinkingConnector, PropertyRepresentationConnector}
import models._
import play.api.libs.json.Json
import play.api.mvc.Action
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream5xxResponse}

import scala.concurrent.Future

class PropertyLinkingController @Inject()(propertyLinksConnector: PropertyLinkingConnector,
                                          groupAccountsConnector: GroupAccountConnector,
                                          representationsConnector: PropertyRepresentationConnector
                                         ) extends PropertyLinkingBaseController {

  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      propertyLinksConnector.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
        .map { _ => Created }
        .recover { case _: Upstream5xxResponse => InternalServerError }
    }
  }

  def find(organisationId: Long, paginationParams: PaginationParams) = Action.async { implicit request =>
    getProperties(organisationId, paginationParams).map(x => Ok(Json.toJson(x)))
  }

  private def getProperties(organisationId: Long, params: PaginationParams)(implicit hc: HeaderCarrier): Future[PropertyLinkResponse] = {
    for {
      view <- propertyLinksConnector.find(organisationId, params)
      detailedLinks <- Future.traverse(view.authorisations)(prop => {
        for {
          optionalGroupAccounts <- Future.traverse(prop.parties)(party => {
            groupAccountsConnector.get(party.authorisedPartyOrganisationId).map(_.map(groupAccount => (party, groupAccount)))
          })
          apiPartiesWithGroupAccounts = optionalGroupAccounts.flatten
          parties = apiPartiesWithGroupAccounts.flatMap { case (p: APIParty, g: GroupAccount) => Party.fromAPIParty(p, g) }
        } yield DetailedPropertyLink.fromAPIAuthorisation(prop, parties)
      })
    } yield {
      PropertyLinkResponse(view.resultCount, detailedLinks)
    }
  }

  def clientProperties(userOrgId: Long, agentOrgId: Long, params: PaginationParams) = Action.async { implicit request =>
    (for {
      view <- propertyLinksConnector.find(userOrgId, params)
      filteredProps = view.authorisations.filter(_.parties.map(_.authorisedPartyOrganisationId).contains(agentOrgId))
      filteredPropsAgents = filteredProps.map(prop => prop.copy(parties = prop.parties.filter(_.authorisedPartyOrganisationId == agentOrgId)))
      userAccount <- groupAccountsConnector.get(view.authorisations.head.authorisationOwnerOrganisationId)
    } yield {
      ClientPropertyResponse(
        view.resultCount,
        filteredPropsAgents.map(x => ClientProperty.build(x, userAccount))
      )
    }).map(x => Ok(Json.toJson(x)))
  }

  def assessments(authorisationId: Long) = Action.async { implicit request =>
    propertyLinksConnector.getAssessment(authorisationId) map { x => Ok(Json.toJson(x)) }
  }
}

