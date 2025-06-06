/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.config

import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URL
import javax.inject.Inject

class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val proxyEnabled: Boolean = configuration.get[Boolean]("http-verbs.proxy.enabled")

  val apimSubscriptionKeyValue: String = configuration.get[String]("extraHeaders.subscriptionKey.value")

  val voaApiBaseUrl: String = configuration.get[String]("voaApiUrl")

  val modernisedBase: URL =
    if (proxyEnabled) new URL(voaApiBaseUrl)
    else new URL(servicesConfig.baseUrl("voa-modernised-api"))

  val bstBase: URL =
    if (proxyEnabled) new URL(voaApiBaseUrl)
    else new URL(servicesConfig.baseUrl("voa-bst"))

}
