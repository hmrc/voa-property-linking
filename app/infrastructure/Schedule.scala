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

package infrastructure

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import play.api.Configuration

import scala.concurrent.duration.FiniteDuration

trait Schedule {
  def timeUntilNextRun(): FiniteDuration
}

class RegularSchedule @Inject() (val configuration: Configuration) extends Schedule {
  def timeUntilNextRun() =
    new FiniteDuration(configuration.getLong("fileTransfer.frequencySeconds").getOrElse(60L),
      TimeUnit.SECONDS)
}

