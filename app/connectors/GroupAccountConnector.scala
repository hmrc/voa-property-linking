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

import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class GroupAccountConnector(http: HttpGet with HttpPost)(implicit ec: ExecutionContext) extends ServicesConfig {

  lazy val url =  baseUrl("external-business-rates-data-platform") + "/groups"

  def create(account: GroupAccountSubmission)(implicit hc: HeaderCarrier): Future[Unit] = {
    http.POST[GroupAccountSubmission, HttpResponse](url, account) map { _ => () }
  }

  def get()(implicit hc: HeaderCarrier): Future[Seq[GroupAccount]] = {
    http.GET[Seq[GroupAccount]](url)
  }

  def get(id: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/$id")
  }

  def withAgentCode(agentCode: String)(implicit hc: HeaderCarrier): Future[Option[GroupAccount]] = {
    http.GET[Option[GroupAccount]](s"$url/agentCode/$agentCode")
  }
}
