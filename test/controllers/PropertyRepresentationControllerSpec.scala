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

package controllers

import basespecs.BaseControllerSpec
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.FakeRequest

import scala.concurrent.Future

class PropertyRepresentationControllerSpec extends BaseControllerSpec {

  trait Setup {
    val testController: PropertyRepresentationController =
      new PropertyRepresentationController(
        authenticated = preAuthenticatedActionBuilders(),
        authorisationManagementApi = mockAuthorisationManagementApi,
        authorisationSearchApi = mockAuthorisationSearchApi,
        customerManagementApi = mockCustomerManagementApi,
        auditingService = mockAuditingService
      )
    val agentCode: Long = 12345L
    val authorisationId: Long = 54321L
    val orgId: Long = 1L
  }

  "create" should {
    "return 200 OK" when {
      "valid JSON payload is POSTed" in new Setup {

        when(mockAuthorisationManagementApi.create(any())(any()))
          .thenReturn(Future.successful(()))

        val result = testController.create()(FakeRequest().withBody(Json.parse(
          """{
            |  "authorisationId" : 1,
            |  "agentOrganisationId" : 2,
            |  "individualId" : 3,
            |  "submissionId" : "A1",
            |  "checkPermission" : "START",
            |  "challengePermission" : "START",
            |  "createDatetime" : "2019-09-12T15:36:47.125Z"
            |}""".stripMargin)))

        status(result) shouldBe OK
      }
    }
  }

  "validateAgentCode" should {
    "return OK 200" when {
      "the agent code is valid" in new Setup {
        when(mockAuthorisationManagementApi.validateAgentCode(mEq(agentCode), mEq(authorisationId))(any()))
          .thenReturn(Future.successful(orgId.asLeft[String]))

        val result = testController.validateAgentCode(agentCode, authorisationId)(FakeRequest())

        status(result) shouldBe OK
        contentAsJson(result) shouldBe Json.obj("organisationId" -> orgId)
      }
    }
  }

}
