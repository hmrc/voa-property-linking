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

import connectors.auth.DefaultAuthConnector
import models.searchApi.{OwnerAgent, OwnerAgents}
import org.mockito.ArgumentMatchers.{any, eq => mockEq}
import org.mockito.Mockito.{reset, when}
import org.scalatest.Outcome
import org.scalatestplus.mockito.MockitoSugar
//import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneAppPerSuite
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{ExecutionContext, Future}

class AgentControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val request = FakeRequest()

  val mockWS = mock[WSHttp]
  val mockConf = mock[ServicesConfig]
  val mockAgentConnector = mock[AgentController]
  val baseUrl = "http://localhost:9999"

  lazy val mockAuthConnector = {
    val m = mock[DefaultAuthConnector]
    when(m.authorise[~[Option[String], Option[String]]](any(), any())(any[HeaderCarrier], any[ExecutionContext])) thenReturn Future.successful(
      new ~(Some("externalId"), Some("groupIdentifier")))
    m
  }

  override lazy val fakeApplication = new GuiceApplicationBuilder()
    .configure("run.mode" -> "Test")
    .overrides(bind[WSHttp].qualifiedWith("VoaBackendWsHttp").toInstance(mockWS))
    .overrides(bind[ServicesConfig].toInstance(mockConf))
    .overrides(bind[DefaultAuthConnector].toInstance(mockAuthConnector))
    .build()

  "given authorised access, manage agents" should {

    when(mockConf.baseUrl(any())).thenReturn(baseUrl)

    "return owner agents" in {
      reset(mockWS)

      // setup
      val testAgentController = fakeApplication.injector.instanceOf[AgentController]
      val organisationId = 111

      val manageAgentUrl = s"$baseUrl/authorisation-search-api/owners/$organisationId/agents"

      val expected = OwnerAgents(agents = Seq(OwnerAgent("Name1", 1), OwnerAgent("Name2", 2)))
      when(mockWS.GET(mockEq(manageAgentUrl))(any(classOf[HttpReads[OwnerAgents]]), any(), any())).thenReturn(Future(expected))

      // test
      val res = testAgentController.manageAgents(organisationId)(FakeRequest())

      // check
      status(res) shouldBe OK
      val names = Json.parse(contentAsString(res)).as[OwnerAgents].agents

      names shouldBe Seq(OwnerAgent("Name1", 1), OwnerAgent("Name2", 2))
    }
  }
}


