package uk.gov.hmrc.voapropertylinking

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.voapropertylinking.WiremockHelper.{wiremockHost, wiremockPort}

trait BaseIntegrationSpec
  extends AnyWordSpec
    with Matchers
    with GuiceOneAppPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with DefaultAwaitTimeout
    with WiremockHelper {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "microservice.services.voa-modernised-api.host" -> wiremockHost,
    "microservice.services.voa-modernised-api.port" -> wiremockPort.toString,
    "microservice.services.voa-bst.host" -> wiremockHost,
    "microservice.services.voa-bst.port" -> wiremockPort.toString
  )

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
