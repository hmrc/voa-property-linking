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

package utils

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}

import models.mdtp.propertylink.requests.APIPropertyLinkRequest
import models.modernised.Capacity.{Capacity => _, _}
import models.modernised.{Capacity, Evidence, EvidenceType, ProvidedEvidence}
import models.modernised.ProvidedEvidence.{apply => _, _}
import models.voa.propertylinking.requests.CreatePropertyLink
import models.FileInfo


trait FakeObjects {

  val date = LocalDate.parse("2018-09-05")
  val instant = date.atStartOfDay().toInstant(ZoneOffset.UTC)
  val FILE_NAME = "test.pdf"
  val fileInfo = FileInfo(FILE_NAME, "ratesBill")
  val evidence = Evidence(FILE_NAME, EvidenceType.RATES_BILL)
  val apiPropertyLinkRequest = APIPropertyLinkRequest(
    uarn = 11111,
    authorisationOwnerOrganisationId = 2222,
    authorisationOwnerPersonId = 33333,
    createDatetime = Instant.now(),
    authorisationMethod = "RATES_BILL",
    uploadedFiles = Seq(fileInfo),
    submissionId = "44444",
    authorisationOwnerCapacity = "OWNER",
    startDate = date,
    endDate = Some(date))

  val testCreatePropertyLinkFromApiPropertyLinkRequest: CreatePropertyLink = CreatePropertyLink(apiPropertyLinkRequest)

  val testCreatePropertyLink: CreatePropertyLink = CreatePropertyLink(
    uarn = 11111,
    capacity = Capacity.withName("OWNER"),
    startDate = date,
    endDate =  Some(date),
    method = ProvidedEvidence.withName("RATES_BILL"),
    PLsubmissionId = "44444",
    createDatetime = testCreatePropertyLinkFromApiPropertyLinkRequest.createDatetime,
    uploadedFiles =  Seq(evidence),
    submissionSource = "DFE_UI")
}