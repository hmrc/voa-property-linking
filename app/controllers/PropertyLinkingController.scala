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
import uk.gov.voa.businessrates.dataplatform.stub.models.APIAuthorisation

import scala.concurrent.Future

class PropertyLinkingController @Inject() (
                                            propertyLinksConnector: PropertyLinkingConnector,
                                            groupAccountsConnector: GroupAccountConnector,
                                            representationsConnector: PropertyRepresentationConnector
                                          ) extends PropertyLinkingBaseController {

  def resolveGroupIds(ids:Seq[Long])(implicit hc:HeaderCarrier):Future[Map[Long, Option[GroupAccount]]] = {
    Future.sequence(ids.map(id => groupAccountsConnector.get(id).map((id, _)))).map(_.toMap)
  }

  def createDetailedPropertyLink(apiAuth:APIAuthorisation, groupAccountsMap:Map[Long, Option[GroupAccount]]):DetailedPropertyLink = {
    val parties = apiAuth.parties.fold(Seq[Party]())(_.flatMap(apiParty => groupAccountsMap(apiParty.authorisedPartyOrganisationId).map(Party.fromAPIParty(apiParty, _))).flatten)
    DetailedPropertyLink.fromAPIAuthorisation(apiAuth, parties)
  }

  def createDetailedPropertyLink(apiAuth:APIDashboardPropertyView, groupAccountsMap:Map[Long, Option[GroupAccount]]):DetailedPropertyLink = {
    val parties = apiAuth.parties.flatMap(apiParty => groupAccountsMap(apiParty.authorisedPartyOrganisationId).map(Party.fromAPIParty(apiParty, _))).flatten
    DetailedPropertyLink.fromAPIDashboardPropertyView(apiAuth, parties)
  }



  def get(authorisationId: Long) = Action.async { implicit request => {
    for {
      apiAuth <- propertyLinksConnector.get(authorisationId)
      apiParties = apiAuth.parties.getOrElse(Seq[APIParty]())
      groupAccountsMap <- resolveGroupIds(apiParties.map(_.authorisedPartyOrganisationId))
    } yield Ok(Json.toJson(createDetailedPropertyLink(apiAuth, groupAccountsMap)))
  }}


  def create() = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkRequest] { linkRequest =>
      propertyLinksConnector.create(APIPropertyLinkRequest.fromPropertyLinkRequest(linkRequest))
        .map { _ => Created }
        .recover { case _: Upstream5xxResponse => InternalServerError }
    }
  }

  def setEnd(authorisationId: Long) = Action.async(parse.json) { implicit request =>
    withJsonBody[PropertyLinkEndDateRequest] { endRequest =>
      propertyLinksConnector.setEnd(authorisationId, APIPropertyLinkEndDateRequest.fromPropertyLinkEndDateRequest(endRequest)) map { _ =>
        Ok
      }
    }
  }

  private def getProperties(organisationId:Long)(implicit hc: HeaderCarrier): Future[Seq[DetailedPropertyLink]] = {
    for {
      props <- propertyLinksConnector.find(organisationId)
      groupAccountMap <- resolveGroupIds(props.flatMap(_.parties.map(_.authorisedPartyOrganisationId)).distinct)
      res = props.map(p => createDetailedPropertyLink(p, groupAccountMap))
    } yield res
  }

  private def getProperties(organisationId: Long, uarn:Long)(implicit hc: HeaderCarrier): Future[Seq[DetailedPropertyLink]] = {
    for {
      props <- propertyLinksConnector.findFor(organisationId, uarn)
      groupAccountMap <- resolveGroupIds(props.flatMap(_.parties).flatten.map(_.authorisedPartyOrganisationId).distinct)
      res = props.map(p => createDetailedPropertyLink(p, groupAccountMap))
    } yield res
  }


  def getPropertiesWithAgent(organisationId: Long, agentOrgId: Long)(implicit hc: HeaderCarrier): Future[Seq[DetailedPropertyLink]] = {
    for {
      props <- getProperties(organisationId)
      filterProps = props
        .map(p => p.copy(agents = p.agents.filter(_.organisationId == agentOrgId)))
        .filter(_.agents.nonEmpty)
    } yield {
      filterProps
    }
  }

  private def addRepresentedProperties(organisationId: Long, eventualUserProps: Future[Seq[DetailedPropertyLink]])(implicit hc:HeaderCarrier):Future[Seq[DetailedPropertyLink]] = {
    val eventualNominations = representationsConnector.forAgent("APPROVED", organisationId)
    for {
      userProps <- eventualUserProps
      nominations <- eventualNominations
      clientOrgIds = nominations.propertyRepresentations.map(_.organisationId).distinct
      res <- Future.traverse(clientOrgIds)(userOrgId => getPropertiesWithAgent(userOrgId, organisationId))
      managedProperties = res.flatten
    } yield {
      managedProperties ++ userProps
    }
  }

  def find(organisationId: Long) = Action.async { implicit request =>
    val eventualUserProps = getProperties(organisationId)
    val allProps = addRepresentedProperties(organisationId, eventualUserProps)
    allProps.map(x => Ok(Json.toJson(x)))
  }

  def findFor(organisationId: Long, uarn: Long) = Action.async { implicit request => {
      val eventualUserProps = getProperties(organisationId, uarn)
      val allProps = addRepresentedProperties(organisationId, eventualUserProps)
      allProps.map(x => Ok(Json.toJson(x)))
    }
  }

  def clientProperties(userOrgId: Long, agentOrgId: Long) = Action.async { implicit request => {
    (for {
      props <- propertyLinksConnector.find(userOrgId)
      filteredProps = props.filter(_.parties.map(_.authorisedPartyOrganisationId).contains(agentOrgId))
      filteredPropsAgents = filteredProps.map(prop => prop.copy(parties = prop.parties.filter(_.authorisedPartyOrganisationId == agentOrgId)))
      userAccount <- groupAccountsConnector.get(props.head.authorisationOwnerOrganisationId)
    } yield {
      filteredPropsAgents.map(x => ClientProperty.build(x, userAccount))
    }).map(x => Ok(Json.toJson(x)))
  }
  }

  def assessments(authorisationId: Long) = Action.async { implicit request =>
    propertyLinksConnector.getAssessment(authorisationId) map { x => Ok(Json.toJson(x)) }
  }
}

