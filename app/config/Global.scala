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

package config

import java.time.Clock

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import http.VoaHttpClient
import infrastructure.{RegularSchedule, Schedule, VOABackendWSHttp}
import org.joda.time.Duration
import play.api.Mode.Mode
import play.api.{Configuration, Environment}
import uk.gov.hmrc.play.config.ServicesConfig

class GuiceModule(environment: Environment,
                  configuration: Configuration) extends AbstractModule {
  override def configure() = {

    bind(classOf[String]).annotatedWith(Names.named("lockName")).toInstance("FileTransferLock")
    bind(classOf[VoaHttpClient]).annotatedWith(Names.named("VoaAuthedBackendHttp")).to(classOf[VoaHttpClient])
    bind(classOf[Duration]).annotatedWith(Names.named("lockTimeout")).toInstance(
      Duration.standardMinutes(configuration.getLong("fileTransfer.lockMinutes").getOrElse(30L)))

    bind(classOf[ServicesConfig]).toInstance(new ServicesConfig {
      override protected def mode: Mode = environment.mode

      override protected def runModeConfiguration: Configuration = configuration
    })


    bind(classOf[Schedule]).annotatedWith(Names.named("regularSchedule")).to(classOf[RegularSchedule])

    bindConstant().annotatedWith(Names.named("envelopeCollectionName")).to(configuration.getString("envelope.collection.name").get)
    bindConstant().annotatedWith(Names.named("dvrCollectionName")).to(configuration.getString("dvr.collection.name").get)

    bind(classOf[uk.gov.hmrc.play.http.ws.WSHttp]).annotatedWith(Names.named("VoaBackendWsHttp")).to(classOf[VOABackendWSHttp])
    bind(classOf[Clock]).toInstance(Clock.systemUTC())
  }
}
