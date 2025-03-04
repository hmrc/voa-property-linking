/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.controllers

import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc._
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.config.FeatureSwitch
import uk.gov.hmrc.voapropertylinking.connectors.bst.{CCACaseManagementApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{ModernisedCCACaseManagementApi, ModernisedExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.repositories.DVRRecordRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DVRCaseManagement @Inject() (
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      modernisedDvrCaseManagement: ModernisedCCACaseManagementApi,
      modernisedValuationManagementApi: ModernisedExternalValuationManagementApi,
      dvrCaseManagement: CCACaseManagementApi,
      valuationManagementApi: ExternalValuationManagementApi,
      featureSwitch: FeatureSwitch,
      dvrRecordRepository: DVRRecordRepository
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def requestDetailedValuationV2: Action[JsValue] =
    authenticated.async(parse.json) { implicit request =>
      withJsonBody[DetailedValuationRequest] { dvrRequest =>
        logger.info(s"detailed valuation request submitted: ${dvrRequest.submissionId}")

        for {
          _ <- dvrRecordRepository.create(dvrRequest)
          _ <- if (featureSwitch.isBstDownstreamEnabled)
                 dvrCaseManagement.requestDetailedValuation(dvrRequest)
               else
                 modernisedDvrCaseManagement.requestDetailedValuation(dvrRequest)
        } yield Ok
      }
    }

  def getDvrDocuments(
        valuationId: Long,
        uarn: Long,
        propertyLinkId: String
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      lazy val optDvrDocumentFiles =
        if (featureSwitch.isBstDownstreamEnabled)
          valuationManagementApi.getDvrDocuments(valuationId, uarn, propertyLinkId)
        else
          modernisedValuationManagementApi.getDvrDocuments(valuationId, uarn, propertyLinkId)
      optDvrDocumentFiles
        .map {
          case Some(response) =>
            logger.debug(s"dvr documents response: ${Json.prettyPrint(Json.toJson(response))}")
            Ok(Json.toJson(response))
          case None => NotFound
        }
    }

  def getDvrDocument(
        valuationId: Long,
        uarn: Long,
        propertyLinkId: String,
        fileRef: String
  ): Action[AnyContent] =
    authenticated.async { implicit request =>
      lazy val response: Future[WSResponse] =
        if (featureSwitch.isBstDownstreamEnabled)
          valuationManagementApi.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef)
        else
          modernisedValuationManagementApi.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef)
      response
        .map { document =>
          val contentType =
            document.headers.view.mapValues(_.mkString(",")).getOrElse(CONTENT_TYPE, "application/octet-stream")
          Ok.sendEntity(
            HttpEntity.Streamed(
              document.bodyAsSource,
              document.headers.view.mapValues(_.mkString(",")).get(CONTENT_LENGTH).map(_.toLong),
              Some(contentType)
            )
          )
        }
    }

  def getDvrRecord(organisationId: Long, assessmentRef: Long): Action[AnyContent] =
    authenticated.async {
      dvrRecordRepository
        .find(organisationId, assessmentRef)
        .map(_.fold(NotFound(Json.toJson(None)))(record => Ok(Json.toJson(record))))
    }
}
