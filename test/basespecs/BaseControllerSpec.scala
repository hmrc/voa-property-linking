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

package basespecs

import controllers.AgentController
import org.scalatest.{BeforeAndAfterEach, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc._
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.voapropertylinking.actions.AuthenticatedActionBuilder
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.test.AllMocks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseControllerSpec extends WordSpec with FutureAwaits with DefaultAwaitTimeout with BeforeAndAfterEach with MustMatchers with MockitoSugar with AllMocks {

  implicit val request = FakeRequest()

  val mockWS = mock[WSHttp]
  val mockConf = mock[ServicesConfig]
  val mockAgentConnector = mock[AgentController]
  val baseUrl = "http://localhost:9999"

  def preAuthenticatedActionBuilders(
                                      externalId: String = "gg_external_id",
                                      groupId: String = "gg_group_id"
                                    ): AuthenticatedActionBuilder =
    new AuthenticatedActionBuilder(mock[AuthConnector]) {

      override def invokeBlock[A](request: Request[A], block: RequestWithPrincipal[A] => Future[Result]): Future[Result] = {
        block(RequestWithPrincipal(request, Principal(externalId, groupId)))
      }
    }
}