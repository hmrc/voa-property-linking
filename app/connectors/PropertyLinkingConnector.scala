/*
 * Copyright 2016 HM Revenue & Customs
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

package connectors

import connectors.ServiceContract.PropertyLink
import play.api.Logger
import serialization.JsonFormats._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform") + "/property-links"

  def create(submissionId: String, link: PropertyLink)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/${link.uarn}/${link.userId}/$submissionId"
    http.POST[PropertyLink, HttpResponse](url, link) map { _ => () }
  }

  def get(userId: String)(implicit hc: HeaderCarrier): Future[Seq[PropertyLink]] = {
    val url = baseUrl + s"/$userId"
    http.GET[Seq[PropertyLink]](url)
  }

}

