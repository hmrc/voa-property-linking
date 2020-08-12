/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

class GuiceModule(
      environment: Environment,
      configuration: Configuration
) extends AbstractModule {

  val servicesConfig = new ServicesConfig(configuration, new RunMode(configuration, environment.mode))

  override def configure(): Unit = {

    bind(classOf[ServicesConfig]).toInstance(servicesConfig)

    bindConstant().annotatedWith(Names.named("dvrCollectionName")).to(configuration.get[String]("dvr.collection.name"))
    bindConstant()
      .annotatedWith(Names.named("agentQueryParameterEnabledExternal"))
      .to(
        configuration
          .getAndValidate[String]("featureFlags.agentQueryParameterEnabledExternal", Set("true", "false"))
          .toBoolean)

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
        "voa.authValuationHistoryUrl"             -> "voa.resources.externalValuationManagement.valuationHistory.path",
        "voa.myAgentPropertyLinks"                -> "voa.resources.externalPropertyLink.myAgentPropertyLinks.path",
        "voa.myOrganisationsPropertyLinks"        -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLinks.path",
        "voa.myOrganisationsPropertyLink"         -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLink.path",
        "voa.myOrganisationsAgents"               -> "voa.resources.externalPropertyLink.myOrganisationsAgents.path",
        "voa.myClientsPropertyLink"               -> "voa.resources.externalPropertyLink.myClientsPropertyLink.path",
        "voa.myClientsPropertyLinks"              -> "voa.resources.externalPropertyLink.myClientsPropertyLinks.path",
        "voa.myClients"                           -> "voa.resources.externalPropertyLink.myClients.path",
        "voa.createPropertyLink"                  -> "voa.resources.externalPropertyLink.createPropertyLink.path",
        "voa.createPropertyLinkOnClientBehalf"    -> "voa.resources.externalPropertyLink.createPropertyLinkOnClientBehalf.path",
        "voa.revokeClientsPropertyLink"           -> "voa.resources.externalPropertyLink.revokeMyClientsPropertyLink.path",
        "voa.createRepresentationRequest"         -> "voa.resources.authorisationManagementApi.createRepresentationRequest.path",
        "voa.representationRequestResponse"       -> "voa.resources.authorisationManagementApi.representationRequestResponse.path",
        "voa.agentAppointmentChanges"             -> "voa.resources.organisationManagementApi.agentAppointmentChanges.path",
        "voa.myAgentDetails"                      -> "voa.resources.organisationManagementApi.myAgentDetails.path"
      ),
      servicesConfig.baseUrl("voa-modernised-api")
    )

  protected def bindStringWithPrefix(path: String, prefix: String, name: String = ""): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(s"$prefix${configuration.get[String](path)}")

  private def resolveAnnotationName(path: String, name: String): String = name match {
    case "" => path
    case _  => name
  }

  private def configException(path: String) = throw new ConfigException.Missing(path)
}
