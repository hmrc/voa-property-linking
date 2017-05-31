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

import play.api.Play._

object ApplicationConfig {
  val privateBetaPassword = getConfig("betaLogin.password")
  val passwordValidationEnabled = getConfig("betaLogin.validationRequired").toBoolean //disable locally as trueClientIP is not available
  val lockoutMinutes = getConfig("betaLogin.lockoutMinutes").toInt
  val maxAttempts = getConfig("betaLogin.maxAttempts").toInt
  val apiConfigSubscriptionKeyHeader = getConfig("voaApi.subscriptionKeyHeader")
  val apiConfigTraceHeader = getConfig("voaApi.traceHeader")
  val fileTransferInterval = getConfig("fileTransfer.intervalSeconds").toInt * 1000

  private def getConfig(key: String) = configuration.getString(key).getOrElse(throw ConfigMissing(key))
}

private case class ConfigMissing(key: String) extends Exception(s"Missing config for $key")
