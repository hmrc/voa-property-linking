/*
 * Copyright 2017 HM Revenue & Customs
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

package config

import connectors._
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

object Wiring {
  def apply() = play.api.Play.current.global.asInstanceOf[MicroserviceGlobal].wiring
}

abstract class Wiring {
  val http: HttpGet with HttpPut with HttpDelete with HttpPost
  val backendHttp: HttpGet with HttpPut with HttpDelete with HttpPost

  implicit lazy val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
  lazy val propertyRepresentationConnector = new PropertyRepresentationConnector(backendHttp)
  lazy val propertyLinkingConnector = new PropertyLinkingConnector(backendHttp)
  lazy val individualAccounts = new IndividualAccountConnector(backendHttp)
  lazy val groupAccounts = new GroupAccountConnector(backendHttp)
  lazy val addresses = new AddressConnector(backendHttp)
}

object WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName {
  override val hooks: Seq[HttpHook] = NoneRequired
}

object VOABackendWSHttp extends WSHttp with ServicesConfig {
  override val hooks: Seq[HttpHook] = NoneRequired

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    super.doGet(url)(hc.withExtraHeaders(
      ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
      ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
    ))
  }

  override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    super.doDelete(url)(hc.withExtraHeaders(
      ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
      ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
    ))
  }

  override def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    super.doPatch(url, body)(rds, hc.withExtraHeaders(
      ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
      ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
    ))
  }

  override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    super.doPut(url, body)(rds, hc.withExtraHeaders(
      ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
      ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
    ))
  }

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    super.doPost(url, body, headers)(rds, hc.withExtraHeaders(
      ("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader),
      ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader)
    ))
  }

}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
