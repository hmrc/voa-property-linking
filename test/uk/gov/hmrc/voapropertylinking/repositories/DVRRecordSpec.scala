package uk.gov.hmrc.voapropertylinking.repositories

import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.DefaultAwaitTimeout

class DVRRecordSpec extends WordSpec with GuiceOneAppPerSuite with DefaultAwaitTimeout with ScalaFutures {

  override implicit val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(20, Millis))

  trait Setup {
    val repo: DVRRecordRepository = app.injector.instanceOf[DVRRecordRepository]
  }

  "load from repository" should {
    "should load all records" in new Setup {
      val items: List[DVRRecord] = repo.findAll().futureValue
      items.foreach( println(_))
    }
  }
}