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

import java.net.URI
import javax.inject.{Inject, Singleton}

import com.kenshoo.play.metrics.Metrics
import metrics.HasMetrics
import play.api.libs.json.Writes
import uk.gov.hmrc.play.audit.http.config.LoadAuditingConfig
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.auth.microservice.connectors.AuthConnector
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http._
import uk.gov.hmrc.play.http.hooks.HttpHook
import uk.gov.hmrc.play.http.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class WSHttp extends WSGet with WSPut with WSPost with WSDelete with WSPatch with AppName {
  override val hooks: Seq[HttpHook] = NoneRequired
}

@Singleton
class VOABackendWSHttp @Inject()(override val metrics: Metrics) extends WSHttp with ServicesConfig with HasMetrics {
  override val hooks: Seq[HttpHook] = NoneRequired

  def buildHeaderCarrier(hc: HeaderCarrier): HeaderCarrier = HeaderCarrier(requestId = hc.requestId, sessionId = hc.sessionId)
    .withExtraHeaders(("Ocp-Apim-Subscription-Key", ApplicationConfig.apiConfigSubscriptionKeyHeader), ("Ocp-Apim-Trace", ApplicationConfig.apiConfigTraceHeader))
    .withExtraHeaders(hc.extraHeaders: _*)

  def completeRequestTimer(response: HttpResponse, timer: MetricsTimer): HttpResponse =
    response.status.toString match {
      case status if status.startsWith("2") =>
        timer.completeTimerAndMarkAsSuccess
        response
      case _ =>
        timer.completeTimerAndMarkAsFailure
        response
    }

  def getPath(url: String): String = {
    val path = new URI(url).getPath.drop(1)
    path.substring(0, path.indexOf("/"))
  }

  override def doGet(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    withMetricsTimer(getPath(url)) {
      timer => {
        super.doGet(url)(buildHeaderCarrier(hc)) map {
          response => completeRequestTimer(response, timer)
        }
      }
    }
  }

  override def doDelete(url: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    withMetricsTimer(getPath(url)) {
      timer => {
        super.doDelete(url)(buildHeaderCarrier(hc)) map {
          response => completeRequestTimer(response, timer)
        }
      }
    }
  }

  override def doPatch[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    withMetricsTimer(getPath(url)) {
      timer => {
        super.doPatch(url, body)(rds, buildHeaderCarrier(hc)) map {
          response => completeRequestTimer(response, timer)
        }
      }
    }
  }

  override def doPut[A](url: String, body: A)(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    withMetricsTimer(getPath(url)) {
      timer => {
        super.doPut(url, body)(rds, buildHeaderCarrier(hc)) map {
          response => completeRequestTimer(response, timer)
        }
      }
    }
  }

  override def doPost[A](url: String, body: A, headers: Seq[(String, String)])(implicit rds: Writes[A], hc: HeaderCarrier): Future[HttpResponse] = {
    withMetricsTimer(getPath(url)) {
      timer => {
        super.doPost(url, body, headers)(rds, buildHeaderCarrier(hc)) map {
          response => completeRequestTimer(response, timer)
        }
      }
    }
  }
}

object MicroserviceAuditConnector extends AuditConnector with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"auditing")
}

object MicroserviceAuthConnector extends AuthConnector with ServicesConfig {
  override val authBaseUrl = baseUrl("auth")
}
