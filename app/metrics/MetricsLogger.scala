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

package metrics

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import play.api.libs.ws.{StreamedResponse, WSResponse}
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

trait MetricsLogger {
  val metrics: Metrics
  lazy val registry: MetricRegistry = metrics.defaultRegistry

  implicit val intToString: Int => String = _.toString

  def logMetrics[T](baseName: String)(implicit ec: ExecutionContext): PartialFunction[Try[T], Future[Unit]] = log(mark(baseName, _, 1))
  def logMetrics(baseName: String, name: String, count: Int = 1)(implicit ec: ExecutionContext): Future[Unit] = mark(baseName, name, count)(ec)

  private def log[T](fn: String => Future[Unit]): PartialFunction[Try[T], Future[Unit]] = {
    case Success(r: WSResponse) => fn(r.status)
    case Success(r: HttpResponse) => fn(r.status)
    case Success(StreamedResponse(r, _)) => fn(r.status)
    case Success(_) => fn("unknownSuccess")
    case Failure(ex: HttpException) => fn(ex.responseCode)
    case Failure(ex: Upstream5xxResponse) => fn(ex.upstreamResponseCode)
    case Failure(ex: Upstream4xxResponse) => fn(ex.upstreamResponseCode)
    case Failure(ex) => fn(ex.getClass.getSimpleName)
  }

  private def mark(baseName: String, name: String, count: Int)(implicit ec: ExecutionContext) = Future(registry.meter(s"$baseName.$name").mark(count))
}
