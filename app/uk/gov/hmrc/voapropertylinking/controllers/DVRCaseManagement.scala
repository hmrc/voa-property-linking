/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import play.api.http.HttpEntity
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{CCACaseManagementApi, ExternalValuationManagementApi}
import uk.gov.hmrc.voapropertylinking.repositories.DVRRecordRepository

import scala.concurrent.ExecutionContext

class DVRCaseManagement @Inject()(
      controllerComponents: ControllerComponents,
      authenticated: AuthenticatedActionBuilder,
      dvrCaseManagementV2: CCACaseManagementApi,
      externalValuationManagementApi: ExternalValuationManagementApi,
      dvrRecordRepository: DVRRecordRepository
)(implicit executionContext: ExecutionContext)
    extends PropertyLinkingBaseController(controllerComponents) {

  def requestDetailedValuationV2: Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[DetailedValuationRequest] { dvrRequest =>
      logger.info(s"detailed valuation request submitted: ${dvrRequest.submissionId}")
      for {
        _ <- dvrRecordRepository.create(dvrRequest)
        _ <- dvrCaseManagementV2.requestDetailedValuation(dvrRequest)
      } yield Ok
    }
  }

  def getDvrDocuments(
        valuationId: Long,
        uarn: Long,
        propertyLinkId: String
  ): Action[AnyContent] = authenticated.async { implicit request =>
    externalValuationManagementApi
      .getDvrDocuments(valuationId, uarn, propertyLinkId)
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
  ): Action[AnyContent] = authenticated.async { implicit request =>
    externalValuationManagementApi
      .getDvrDocument(valuationId, uarn, propertyLinkId, fileRef)
      .map { document =>
        val contentType =
          document.headers.mapValues(_.mkString(",")).getOrElse(CONTENT_TYPE, "application/octet-stream")

        Ok.sendEntity(
          HttpEntity.Streamed(
            document.bodyAsSource,
            document.headers.mapValues(_.mkString(",")).get(CONTENT_LENGTH).map(_.toLong),
            Some(contentType)))
      }
  }

  def dvrExists(organisationId: Long, assessmentRef: Long): Action[AnyContent] = authenticated.async {
    dvrRecordRepository.exists(organisationId, assessmentRef).map(res => Ok(Json.toJson(res)))
  }
}
