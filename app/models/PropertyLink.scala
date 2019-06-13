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

package models

import java.time.{LocalDate, ZoneId}

import play.api.libs.json.{Format, Json}

case class PropertyLinkResponse(resultCount: Option[Int], propertyLinks: Seq[PropertyLink])

case class PropertyLink(authorisationId: Long,
                        submissionId: String,
                        uarn: Long,
                        address: String,
                        pending: Boolean,
                        assessments: Seq[Assessment],
                        agents: Seq[Party])

object PropertyLink {
  implicit val formats: Format[PropertyLink] = Json.format[PropertyLink]

  def fromAPIAuthorisation(prop: PropertiesView, parties: Seq[Party]) = {
    PropertyLink(
      prop.authorisationId,
      prop.submissionId,
      prop.uarn,
      prop.NDRListValuationHistoryItems.headOption.map(_.address).getOrElse("No address found"),
      prop.authorisationStatus != "APPROVED",
      prop.NDRListValuationHistoryItems.map(x => Assessment.fromAPIValuationHistory(x, prop.authorisationId)),
      parties
    )
  }
}

object PropertyLinkResponse {
  implicit val formats: Format[PropertyLinkResponse] = Json.format[PropertyLinkResponse]
}
