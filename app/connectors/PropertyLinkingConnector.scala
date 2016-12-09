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

import models.{DetailedPropertyLinkRead, PropertyLink, PropertyLinkRequest}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.{ExecutionContext, Future}

class PropertyLinkingConnector(http: HttpGet with HttpPut with HttpPost)(implicit ec: ExecutionContext)
  extends ServicesConfig {
  lazy val baseUrl: String = baseUrl("external-business-rates-data-platform")

  def create(linkId: String, linkingRequest: PropertyLinkRequest)(implicit hc: HeaderCarrier): Future[Unit] = {
    val url = baseUrl + s"/property-links/$linkId"
    http.POST[PropertyLinkRequest, HttpResponse](url, linkingRequest) map { _ => () }
  }

  def find(groupId: String)(implicit hc: HeaderCarrier): Future[Seq[DetailedPropertyLinkRead]] = {
    val url = baseUrl + s"/dashboard/properties/$groupId"
    http.GET[Seq[DetailedPropertyLinkRead]](url)
  }

  def get(linkId: String)(implicit hc: HeaderCarrier): Future[Option[PropertyLink]] = {
    val url = baseUrl + s"/property-links/$linkId"
    http.GET[Option[PropertyLink]](url)
  }
}

