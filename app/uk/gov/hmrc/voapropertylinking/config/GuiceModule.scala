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
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class GuiceModule(
      environment: Environment,
      configuration: Configuration
) extends AbstractModule {

  lazy val servicesConfig = new ServicesConfig(configuration)

  override def configure(): Unit = {

    bind(classOf[ServicesConfig]).toInstance(servicesConfig)

    bindConstant().annotatedWith(Names.named("dvrCollectionName")).to(configuration.get[String]("dvr.collection.name"))

    bind(classOf[Clock]).toInstance(Clock.systemUTC())

    bindBstEndpoints()
    bindModernisedEndpoints()
  }

  private def bindEndpoints(endpoints: Map[String, String], baseUrl: String): Unit =
    endpoints.toList.foreach { case (boundName, configPath) =>
      bindStringWithPrefix(configPath, baseUrl, boundName)
    }

  private def bindBstEndpoints(): Unit =
    bindEndpoints(
      Map(
        "voa.authValuationHistoryUrl"       -> "bst.resources.externalValuationManagement.valuationHistory.path",
        "voa.myAgentPropertyLinks"          -> "bst.resources.externalPropertyLink.myAgentPropertyLinks.path",
        "voa.myAgentAvailablePropertyLinks" -> "bst.resources.externalPropertyLink.myAgentAvailablePropertyLinks.path",
        "voa.myOrganisationsPropertyLinks"  -> "bst.resources.externalPropertyLink.myOrganisationsPropertyLinks.path",
        "voa.myOrganisationsPropertyLink"   -> "bst.resources.externalPropertyLink.myOrganisationsPropertyLink.path",
        "voa.myOrganisationsAgents"         -> "bst.resources.externalPropertyLink.myOrganisationsAgents.path",
        "voa.myClientsPropertyLink"         -> "bst.resources.externalPropertyLink.myClientsPropertyLink.path",
        "voa.myClientsPropertyLinks"        -> "bst.resources.externalPropertyLink.myClientsPropertyLinks.path",
        "voa.myClientPropertyLinks"         -> "bst.resources.externalPropertyLink.myClientPropertyLinks.path",
        "voa.myClients"                     -> "bst.resources.externalPropertyLink.myClients.path",
        "voa.createPropertyLink"            -> "bst.resources.externalPropertyLink.createPropertyLink.path",
        "voa.createPropertyLinkOnClientBehalf" -> "bst.resources.externalPropertyLink.createPropertyLinkOnClientBehalf.path",
        "voa.revokeClientsPropertyLink" -> "bst.resources.externalPropertyLink.revokeMyClientsPropertyLink.path",
        "voa.agentAppointmentChanges"   -> "bst.resources.organisationManagementApi.agentAppointmentChanges.path",
        "voa.myAgentDetails"            -> "bst.resources.organisationManagementApi.myAgentDetails.path"
      ),
      servicesConfig.baseUrl("voa-bst")
    )

  private def bindModernisedEndpoints(): Unit =
    bindEndpoints(
      Map(
        "voa.modernised.authValuationHistoryUrl" -> "voa.resources.externalValuationManagement.valuationHistory.path",
        "voa.modernised.myAgentPropertyLinks"    -> "voa.resources.externalPropertyLink.myAgentPropertyLinks.path",
        "voa.modernised.myAgentAvailablePropertyLinks" -> "voa.resources.externalPropertyLink.myAgentAvailablePropertyLinks.path",
        "voa.modernised.myOrganisationsPropertyLinks" -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLinks.path",
        "voa.modernised.myOrganisationsPropertyLink" -> "voa.resources.externalPropertyLink.myOrganisationsPropertyLink.path",
        "voa.modernised.myOrganisationsAgents"  -> "voa.resources.externalPropertyLink.myOrganisationsAgents.path",
        "voa.modernised.myClientsPropertyLink"  -> "voa.resources.externalPropertyLink.myClientsPropertyLink.path",
        "voa.modernised.myClientsPropertyLinks" -> "voa.resources.externalPropertyLink.myClientsPropertyLinks.path",
        "voa.modernised.myClientPropertyLinks"  -> "voa.resources.externalPropertyLink.myClientPropertyLinks.path",
        "voa.modernised.myClients"              -> "voa.resources.externalPropertyLink.myClients.path",
        "voa.modernised.createPropertyLink"     -> "voa.resources.externalPropertyLink.createPropertyLink.path",
        "voa.modernised.createPropertyLinkOnClientBehalf" -> "voa.resources.externalPropertyLink.createPropertyLinkOnClientBehalf.path",
        "voa.modernised.revokeClientsPropertyLink" -> "voa.resources.externalPropertyLink.revokeMyClientsPropertyLink.path",
        "voa.modernised.agentAppointmentChanges" -> "voa.resources.organisationManagementApi.agentAppointmentChanges.path",
        "voa.modernised.myAgentDetails" -> "voa.resources.organisationManagementApi.myAgentDetails.path"
      ),
      servicesConfig.baseUrl("voa-modernised-api")
    )

  protected def bindStringWithPrefix(path: String, prefix: String, name: String = ""): Unit =
    bindConstant()
      .annotatedWith(named(resolveAnnotationName(path, name)))
      .to(s"$prefix${configuration.get[String](path)}")

  private def resolveAnnotationName(path: String, name: String): String =
    name match {
      case "" => path
      case _  => name
    }

}
