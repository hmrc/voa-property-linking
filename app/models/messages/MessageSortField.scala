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

package models.messages

import models.{NamedEnum, NamedEnumSupport}

sealed trait MessageSortField extends NamedEnum {
  val apiQueryString: String
}

object MessageSortField extends NamedEnumSupport[MessageSortField] {
  case object EffectiveDate extends MessageSortField {
    override val name: String = "effectiveDate"
    override val apiQueryString: String = "EFFECTIVEDATE"
  }

  case object Address extends MessageSortField {
    override val name: String = "address"
    override val apiQueryString: String = "ADDRESS"
  }

  case object CaseReference extends MessageSortField {
    override val apiQueryString: String = "BUSINESSKEY1"
    override val name: String = "caseReference"
  }

  case object LastRead extends MessageSortField {
    override val apiQueryString: String = "LASTREADBY"
    override val name: String = "lastRead"
  }

  case object Subject extends MessageSortField {
    override val apiQueryString: String = "SUBJECT"
    override def name: String = "subject"
  }

  case object ClientName extends MessageSortField {
    override val apiQueryString: String = "CLIENTORGANISATIONNAME"
    override def name: String = "clientName"
  }

  case object AgentName extends MessageSortField {
    override val apiQueryString: String = "AGENTORGANISATIONNAME"
    override def name: String = "agentName"
  }

  override def all: Seq[MessageSortField] = Seq(EffectiveDate, Address, CaseReference, LastRead, Subject, ClientName, AgentName)
}
