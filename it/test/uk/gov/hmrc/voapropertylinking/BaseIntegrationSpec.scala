/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.voapropertylinking.WiremockHelper.{wiremockHost, wiremockPort}
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}

trait BaseIntegrationSpec
    extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with BeforeAndAfterEach with BeforeAndAfterAll
    with DefaultAwaitTimeout with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  def config: Map[String, String] =
    Map(
      "auditing.enabled"                              -> "false",
      "microservice.services.voa-modernised-api.host" -> wiremockHost,
      "microservice.services.voa-modernised-api.port" -> wiremockPort.toString,
      "microservice.services.voa-bst.host"            -> wiremockHost,
      "microservice.services.voa-bst.port"            -> wiremockPort.toString
    )

  implicit val requestWithPrincipal: RequestWithPrincipal[_] =
    RequestWithPrincipal(FakeRequest(), principal = Principal("externalId", "groupId"))

  override def beforeAll(): Unit = {
    startWiremock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    resetWiremock()
    super.beforeEach()
  }
}
