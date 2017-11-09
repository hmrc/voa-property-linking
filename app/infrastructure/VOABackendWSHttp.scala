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

package infrastructure

import javax.inject.Inject

import com.kenshoo.play.metrics.Metrics
import metrics.HasMetrics
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.http.ws._

class VOABackendWSHttp @Inject()(override val metrics: Metrics) extends HasMetrics with AzureHeaders with HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with HttpPatch with WSPatch with AppName with HttpHooks{
  override val hooks = NoneRequired
}
