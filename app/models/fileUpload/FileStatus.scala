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

package models.fileUpload

import models.{NamedEnum, NamedEnumSupport}

sealed trait FileStatus extends NamedEnum

case object Quarantined extends FileStatus {
  override def name: String = "QUARANTINED"
}

case object Cleaned extends FileStatus {
  override def name: String = "CLEANED"
}

case object Available extends FileStatus {
  override def name: String = "AVAILABLE"
}

case object Error extends FileStatus {
  override def name: String = "UnKnownFileStatusERROR"
}

object FileStatus extends NamedEnumSupport[FileStatus] {
  override def all: Seq[FileStatus] = Seq(Quarantined, Cleaned, Available, Error)
}
