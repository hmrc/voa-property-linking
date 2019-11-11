/*
 * Copyright 2019 HM Revenue & Customs
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

/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.Clock

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.google.inject.name.Names.named
import com.typesafe.config.ConfigException
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule with ServicesConfig {
  override def configure(): Unit = {

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig {
      override protected def mode: Mode = environment.mode

      override protected def runModeConfiguration: Configuration = configuration
    })

    bindConstant().annotatedWith(Names.named("dvrCollectionName")).to(configuration.getString("dvr.collection.name").get)
    bindConstant().annotatedWith(Names.named("authedAssessmentEndpointEnabled")).to(configuration.getString("featureFlags.authedAssessmentEndpointEnabled").fold(false)(_.toBoolean))
    bindConstant().annotatedWith(Names.named("agentQueryParameterEnabledExternal")).to(configuration.getString("featureFlags.agentQueryParameterEnabledExternal").fold(false)(_.toBoolean))

    bind(classOf[Clock]).toInstance(Clock.systemUTC())

    bindModernisedEndpoints()
  }

  private def bindEndpoints(endpoints: Map[String, String], baseUrl: String): Unit =
    endpoints.toList.foreach {
      case (boundName, configPath) => bindStringWithPrefix(configPath, baseUrl, boundName)
    }

  private def bindModernisedEndpoints(): Unit =
    bindEndpoints(
      Map(
        "voa.authValuationHistoryUrl"      -> "voa.resources.externalValuationManagement.valuationHistory.path",
        "voa.myOrganisationsPropertyLinks" -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLinks.path",
        "voa.myOrganisationsPropertyLink"  -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLink.path",
        "voa.myClientsPropertyLink"        -> "voa.resources.externalPropertyLink.myClientsPropertyLink.path",
        "voa.myClientsPropertyLinks"       -> "voa.resources.externalPropertyLink.myClientsPropertyLinks.path",
        "voa.createPropertyLink"           -> "voa.resources.externalPropertyLink.createPropertyLink.path"
      ),
      baseUrl("voa-modernised-api")
    )

  protected def bindStringWithPrefix(path: String, prefix: String, name: String = ""): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(s"$prefix${configuration.getString(path).getOrElse(configException(path))}")

  private def resolveAnnotationName(path: String, name: String): String = name match {
    case "" => path
    case _  => name
  }

  private def configException(path: String) = throw new ConfigException.Missing(path)

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = configuration
}
