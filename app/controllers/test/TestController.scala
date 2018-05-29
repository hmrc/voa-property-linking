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

package controllers.test

import auth.Authenticated
import connectors.auth.AuthConnector
import connectors.test.TestConnector
import controllers.PropertyLinkingBaseController
import javax.inject.Inject

class TestController @Inject()(val auth: AuthConnector,
                               testConnector: TestConnector)
  extends PropertyLinkingBaseController with Authenticated {

  def deleteOrganisation(organisationId: Long) = authenticated { implicit request =>
    testConnector.deleteOrganisation(organisationId).map(res => Ok)
  }

}
