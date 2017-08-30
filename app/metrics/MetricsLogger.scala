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

package metrics

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.libs.ws.{StreamedResponse, WSResponse}
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MetricsLogger {
  val metrics: Metrics
  lazy val registry: MetricRegistry = metrics.defaultRegistry

  def logMetrics[T](name: String)(implicit ec: ExecutionContext): PartialFunction[Try[T], Future[Unit]] = {
    implicit val _ = name
    implicit val count = 1

    {
      case Success(r: WSResponse) => mark(r.status)
      case Success(r: HttpResponse) => mark(r.status)
      case Success(StreamedResponse(r, _)) => mark(r.status)
      case Success(_) => mark("unknownSuccess")
      case Failure(ex: HttpException) => mark(ex.responseCode)
      case Failure(ex: Upstream5xxResponse) => mark(ex.upstreamResponseCode)
      case Failure(ex: Upstream4xxResponse) => mark(ex.upstreamResponseCode)
      case Failure(ex) => mark(ex.getClass.getSimpleName)
    }
  }

  def logMetrics(baseName: String, name: String, count: Int = 1)(implicit ec: ExecutionContext): Future[Unit] = mark(name)(baseName, count, ec)

  private def mark(status: Int)(implicit key: String, count: Int, ec: ExecutionContext) = Future(registry.meter(s"$key.$status").mark(count))
  private def mark(status: String)(implicit key: String, count: Int, ec: ExecutionContext) = Future(registry.meter(s"$key.$status").mark(count))
}
