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
import java.time.Clock
import javax.inject._

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.typesafe.config.Config
import infrastructure.{RegularSchedule, Schedule, SimpleWSHttp, VOABackendWSHttp}
import net.ceedubs.ficus.Ficus._
import org.joda.time.Duration
import play.api.{Application, Configuration, Environment, Play}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import uk.gov.hmrc.play.audit.filters.AuditFilter
import uk.gov.hmrc.play.auth.controllers.AuthParamsControllerConfig
import uk.gov.hmrc.play.auth.microservice.filters.AuthorisationFilter
import uk.gov.hmrc.play.config.inject.ConfigModule
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.logging.filters.LoggingFilter
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object AuthParamsControllerConfiguration extends AuthParamsControllerConfig {
  lazy val controllerConfigs = ControllerConfiguration.controllerConfigs
}

object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector
  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceAuthFilter extends AuthorisationFilter with MicroserviceFilterSupport {
  override lazy val authParamsConfig = AuthParamsControllerConfiguration
  override lazy val authConnector = MicroserviceAuthConnector
  override def controllerNeedsAuth(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuth
}

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule {
  override def configure() = {
    bind(classOf[String]).annotatedWith(Names.named("lockName")).toInstance("FileTransferLock")
    bind(classOf[Duration]).annotatedWith(Names.named("lockTimeout")).toInstance(
      Duration.standardMinutes(configuration.getLong("fileTransfer.lockMinutes").getOrElse(30L)))

    bind(classOf[Schedule]).annotatedWith(Names.named("regularSchedule")).to(classOf[RegularSchedule])
    bind(classOf[DB]).toProvider(classOf[MongoDbProvider]).asEagerSingleton()
    
    bindConstant().annotatedWith(Names.named("voaApiSubscriptionHeader")).to(configuration.getString("voaApi.subscriptionKeyHeader").get)
    bindConstant().annotatedWith(Names.named("voaApiTraceHeader")).to(configuration.getString("voaApi.traceHeader").get)
    bindConstant().annotatedWith(Names.named("envelopeCollectionName")).to(configuration.getString("envelope.collection.name").get)

    bind(classOf[WSHttp]).annotatedWith(Names.named("VoaBackendWsHttp")).to(classOf[VOABackendWSHttp])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
  }
}

class MongoDbProvider @Inject() (reactiveMongoComponent: ReactiveMongoComponent) extends Provider[DB] {
  def get = reactiveMongoComponent.mongoConnector.db()
}

object Global extends MicroserviceGlobal {

}

trait MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode {
  override val auditConnector = MicroserviceAuditConnector

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = Some(MicroserviceAuthFilter)

}
