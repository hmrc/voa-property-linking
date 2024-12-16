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

package models.voa.propertylinking.requests

import basespecs.BaseUnitSpec
import models.FileInfo
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import models.modernised.{Capacity, Evidence, EvidenceType, ProvidedEvidence}

class CreatePropertyLinkSpec extends BaseUnitSpec {

  "CreatePropertyLink" should {
    "create from ApiPropertyLinkRequest " in {

      val fileInfo = FileInfo(FILE_NAME, "serviceCharge")
      val evidence = Evidence(FILE_NAME, EvidenceType.SERVICE_CHARGE)

      val apiRequest = apiPropertyLinkRequest.copy(
        uploadedFiles = Seq(fileInfo),
        authorisationMethod = "NO_EVIDENCE",
        authorisationOwnerCapacity = "OCCUPIER"
      )
      val expectedRequest: CreatePropertyLink = testCreatePropertyLink.copy(
        uploadedFiles = Seq(evidence),
        method = ProvidedEvidence.withName("NO_EVIDENCE"),
        capacity = Capacity.withName("OCCUPIER")
      )

      val targetRequest: CreatePropertyLink = CreatePropertyLink(apiRequest)

      targetRequest.uploadedFiles shouldBe expectedRequest.uploadedFiles
      targetRequest.method shouldBe expectedRequest.method
      targetRequest.capacity shouldBe expectedRequest.capacity
    }
  }
}
