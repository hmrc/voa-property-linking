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

package test

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID.randomUUID

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import connectors.{AddressConnector, GroupAccountConnector, IndividualAccountConnector}
import infrastructure.VOABackendWSHttp
import org.mockito.ArgumentMatchers.{eq => matches}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mock.MockitoSugar
import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.inject.ServicesConfig
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object TestCreateAccountService extends MockitoSugar {
  class DisabledMetrics extends Metrics {
    override def defaultRegistry: MetricRegistry = new MetricRegistry()
    override def toJson: String = "{}"
  }

  case class KnownFact(key:String, value:String)

  object KnownFact {
    implicit val format = Json.format[KnownFact]
  }

  case class Enrolment(key:String, identifier:String, state:String)

  object Enrolment {
    implicit val format = Json.format[Enrolment]

    val ACTIVATED = "Activated"
    val NOT_YET_ACTIVATED = "NotYetActivated"
  }

  case class GovernmentGatewayUser(
    name:String, username:String, password:String, role:String, affinityGroup:String,
    credentialIdentifier:String, groupIdentifier:String, enrolments:List[Enrolment],
    email:String, allEnrolments:List[Enrolment], knownFacts:List[KnownFact])

  object GovernmentGatewayUser {
    implicit val format = Json.format[GovernmentGatewayUser]
  }

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val clock:java.time.Clock = Clock.fixed(Instant.now, ZoneId.systemDefault)

  val ggStubUrl = "http://localhost:8082"
  val dpStubUrl = "http://localhost:9536"

  val http = new VOABackendWSHttp(new DisabledMetrics())
  val config = mock[ServicesConfig]
  when(config.baseUrl(matches("external-business-rates-data-platform"))).thenReturn(dpStubUrl)
  val addresses = new AddressConnector(http, config)
  val individuals = new IndividualAccountConnector(addresses, http)
  val groups = new GroupAccountConnector(http, config)

  def createUser(
    username:String, password:String, groupId:String, credential:String,
    firstName:String="Test", lastName:String, email:String="test.user@mail.com") = {

    val affinityGroup = "Organisation"
    val identifier = randomUUID().toString()

    val enrolments = List(Enrolment(key="HMRC-VOA-CCA", identifier=identifier, state=Enrolment.ACTIVATED))
    val user = GovernmentGatewayUser(
      name=s"$firstName $lastName", username=username, password=password, role="User",
      affinityGroup=affinityGroup, credentialIdentifier=credential, knownFacts=List[KnownFact](),
      groupIdentifier=groupId, enrolments=enrolments, allEnrolments=enrolments, email=email)
    http.POST[GovernmentGatewayUser, HttpResponse](s"$ggStubUrl/test-only/users", user, Seq("Content-Type"->"application/json"))
  }
}

class TestCreateAccountServiceSpec extends FlatSpec with MustMatchers with MockitoSugar with ScalaFutures with WithFakeApplication {
  val service = TestCreateAccountService

  // NOTE for test to pass, GG stubs needs to be started with -Dfeature.dynamicUsers=true
  ignore should "Create user under org ready for login" in {
    val id = 0
    val result = service.createUser(
      username=s"user$id", password=s"pass$id", lastName=s"User $id",
      groupId="stub-group-3", credential=s"cred$id", email=s"test.user$id@mail.com")
    Await.result(result, 10 seconds)
  }
}