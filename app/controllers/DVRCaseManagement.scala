/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.Authenticated
import connectors.DVRCaseManagementConnector
import connectors.auth.{AuthConnector, DefaultAuthConnector}
import javax.inject.Inject

import models.DetailedValuationRequest
import play.api.libs.json.Json
import play.api.mvc.Action
import repositories.DVRRecordRepository
import play.api.Logger

class DVRCaseManagement @Inject()(val authConnector: DefaultAuthConnector,
                                  dvrCaseManagement: DVRCaseManagementConnector,
                                  dvrRecordRepository: DVRRecordRepository)
  extends PropertyLinkingBaseController with Authenticated {

  def requestDetailedValuation = authenticated(parse.json) { implicit request =>
    withJsonBody[DetailedValuationRequest] { dvr => {
      Logger.info(s"detailed valuation request submitted: ${dvr.submissionId}")
      dvrRecordRepository.create(dvr.organisationId, dvr.assessmentRef).flatMap(_ =>
        dvrCaseManagement.requestDetailedValuation(dvr) map { _ => Ok })
    }
    }
  }

  def dvrExists(organisationId: Long, assessmentRef: Long) = Action.async { implicit request =>
    dvrRecordRepository.exists(organisationId, assessmentRef).map {
      case true => Ok(Json.toJson(true))
      case false => Ok(Json.toJson(false))
    }
  }

}

