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

import models.SortOrder
import play.api.mvc.QueryStringBindable

case class MessageSearchParams(recipientOrgId: Long,
                               clientOrgId: Option[Long],
                               clientName: Option[String],
                               referenceNumber: Option[String],
                               address: Option[String],
                               sortField: MessageSortField,
                               sortOrder: SortOrder,
                               startPoint: Int,
                               pageSize: Int) {

  lazy val apiQueryString: String = {
    s"""
      |recipientOrganisationID=$recipientOrgId&
      |${clientOrgId.fold("")(id => s"clientOrganisationID=$id&")}
      |${clientName.fold("")(cn => s"clientOrganisationName=$cn&")}
      |${referenceNumber.fold("")(rn => s"businessKey1=$rn&")}
      |${address.fold("")(a => s"address=$a&")}
      |sortField=${sortField.apiQueryString}&
      |sortOrder=${sortOrder.apiQueryString}&
      |start=$startPoint&
      |size=$pageSize
      |""".stripMargin.replaceAll("\n", "")
  }
}

object MessageSearchParams {
  implicit val queryStringBindable: QueryStringBindable[MessageSearchParams] = {
    new QueryStringBindable[MessageSearchParams] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MessageSearchParams]] = {
        def bindParam[T](key: String)(implicit qsb: QueryStringBindable[T]): Option[Either[String, T]] = qsb.bind(key, params)

        for {
          orgId <- bindParam[Long]("recipientOrgId")
          clientOrgId <- bindParam[Option[Long]]("clientOrgId")
          clientName <- bindParam[Option[String]]("clientName")
          refNum <- bindParam[Option[String]]("referenceNumber")
          address <- bindParam[Option[String]]("address")
          sortField <- bindParam[MessageSortField]("sortField")
          sortOrder <- bindParam[SortOrder]("sortOrder")
          startPoint <- bindParam[Int]("startPoint")
          pageSize <- bindParam[Int]("pageSize")
        } yield {
          (orgId, clientOrgId, clientName, refNum, address, sortField, sortOrder, startPoint, pageSize) match {
            case (Right(oid), Right(cid), Right(cn), Right(rn), Right(add), Right(sf), Right(so), Right(sp), Right(ps)) =>
              Right(MessageSearchParams(oid, cid, cn, rn, add, sf, so, sp, ps))
            case _ =>
              Left("Unable to bind to MessageSearchParams")
          }
        }
      }

      override def unbind(key: String, value: MessageSearchParams): String = {
        s"""
          |recipientOrgId=${value.recipientOrgId}&
          |clientOrgId=${value.clientOrgId.fold("")(_.toString)}&
          |clientName=${value.clientName.getOrElse("")}&
          |referenceNumber=${value.referenceNumber.getOrElse("")}&
          |address=${value.address.getOrElse("")}&
          |sortField=${value.sortField}&
          |sortOrder=${value.sortOrder}&
          |startPoint=${value.startPoint}&
          |pageSize=${value.startPoint}
        """.stripMargin.replaceAll("\n", "")
      }
    }
  }
}
