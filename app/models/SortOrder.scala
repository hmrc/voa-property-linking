/*
 * Copyright 2018 HM Revenue & Customs
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

package models

sealed trait SortOrder extends NamedEnum {
  val apiQueryString: String
}

object SortOrder extends NamedEnumSupport[SortOrder] {
  case object Ascending extends SortOrder {
    override val apiQueryString: String = "ASC"
    override def name: String = "asc"
  }

  case object Descending extends SortOrder {
    override val apiQueryString: String = "DESC"
    override def name: String = "desc"
  }

  override def all: Seq[SortOrder] = Seq(Ascending, Descending)
}
