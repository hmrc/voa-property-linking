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

import java.io.{BufferedInputStream, File}

import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import auth.Authenticated
import connectors.{CCACaseManagementApi, DVRCaseManagementConnector, ExternalValuationManagementApi}
import connectors.auth.DefaultAuthConnector
import javax.inject.Inject
import models.dvr.DetailedValuationRequest
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ResponseHeader, Result}
import repositories.DVRRecordRepository
import play.api.Logger
import play.api.http.HttpEntity.Streamed

class DVRCaseManagement @Inject()(val authConnector: DefaultAuthConnector,
                                  dvrCaseManagement: DVRCaseManagementConnector,
                                  dvrCaseManagementV2: CCACaseManagementApi,
                                  externalValuationManagementApi: ExternalValuationManagementApi,
                                  dvrRecordRepository: DVRRecordRepository)
  extends PropertyLinkingBaseController with Authenticated {

  def requestDetailedValuation: Action[JsValue] = authenticated(parse.json) { implicit request =>
    withJsonBody[DetailedValuationRequest] { dvr => {
      Logger.info(s"detailed valuation request submitted: ${dvr.submissionId}")
      dvrRecordRepository.create(dvr.organisationId, dvr.assessmentRef).flatMap(_ =>
        dvrCaseManagement.requestDetailedValuation(dvr) map { _ => Ok })
    }
    }
  }

  def requestDetailedValuationV2: Action[JsValue] = authenticated(parse.json) { implicit request =>
    withJsonBody[DetailedValuationRequest] { dvr => {
      Logger.info(s"detailed valuation request submitted: ${dvr.submissionId}")
      dvrRecordRepository.create(dvr.organisationId, dvr.assessmentRef).flatMap(_ =>
        dvrCaseManagementV2.requestDetailedValuation(dvr) map { _ => Ok })
    }
    }
  }

  def getDvrDocuments(
                       valuationId: Long,
                       uarn: Long,
                       propertyLinkId: String): Action[AnyContent] = authenticated { implicit request =>
    externalValuationManagementApi
      .getDvrDocuments(valuationId, uarn, propertyLinkId)
      .map{
        case Some(response) => Ok(Json.toJson(response))
        case None           => NotFound
      }
  }

  def getDvrDocument(
                      valuationId: Long,
                      uarn: Long,
                      propertyLinkId: String,
                      fileRef: Long): Action[AnyContent] = authenticated { implicit request =>
    externalValuationManagementApi
      .getDvrDocument(valuationId, uarn, propertyLinkId, fileRef)
      .map(document =>
        Result(
          ResponseHeader(200, document.headers),
          Streamed(document.body, document.contentLength, document.contentType))
      )
  }

  def dvrExists(organisationId: Long, assessmentRef: Long) = Action.async { implicit request =>
    dvrRecordRepository.exists(organisationId, assessmentRef).map {
      case true => Ok(Json.toJson(true))
      case false => Ok(Json.toJson(false))
    }
  }

}

