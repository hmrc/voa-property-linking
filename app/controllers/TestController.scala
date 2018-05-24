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
import com.google.inject.name.Named
import connectors.TestConnector
import connectors.auth.AuthConnector
import javax.inject.Inject
import play.api.libs.json.Json

class TestController @Inject() (val auth: AuthConnector,
                                testConnector: TestConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def delete(organisationId: Long) = authenticated { implicit request => {
    val persons = testConnector.delete(organisationId)
    Ok(Json.toJson("Hit backend controller"))}
  }

}
