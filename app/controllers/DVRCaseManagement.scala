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

package controllers

import javax.inject.Inject
import models.voa.valuation.dvr.DetailedValuationRequest
import play.api.Logger
import play.api.http.HttpEntity.Streamed
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ResponseHeader, Result}
import repositories.DVRRecordRepository
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.connectors.modernised.{CCACaseManagementApi, ExternalValuationManagementApi}

import scala.concurrent.ExecutionContext

class DVRCaseManagement @Inject()(
                                   authenticated: AuthenticatedActionBuilder,
                                   dvrCaseManagementV2: CCACaseManagementApi,
                                   externalValuationManagementApi: ExternalValuationManagementApi,
                                   dvrRecordRepository: DVRRecordRepository
                                 )(implicit executionContext: ExecutionContext) extends PropertyLinkingBaseController {

  val logger = Logger(this.getClass.getName)

  def requestDetailedValuationV2: Action[JsValue] = authenticated.async(parse.json) { implicit request =>
    withJsonBody[DetailedValuationRequest] { dvrRequest =>
      Logger.info(s"detailed valuation request submitted: ${dvrRequest.submissionId}")
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
      .map{
        case Some(response) =>
          logger.debug(s"dvr documents response: ${Json.prettyPrint(Json.toJson(response))}")
          Ok(Json.toJson(response))
        case None           => NotFound
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
      .map(document =>
        Result(
          ResponseHeader(200, document.headers),
          Streamed(document.body, document.contentLength, document.contentType))
      )
  }

  def dvrExists(
                 organisationId: Long,
                 assessmentRef: Long
               ): Action[AnyContent] = authenticated.async { implicit request =>
    dvrRecordRepository.exists(organisationId, assessmentRef).map {
      case true   => Ok(Json.toJson(true))
      case false  => Ok(Json.toJson(false))
    }
  }
}

