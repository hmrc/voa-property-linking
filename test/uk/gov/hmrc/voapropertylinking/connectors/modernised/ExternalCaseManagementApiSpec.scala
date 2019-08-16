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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import java.time.LocalDateTime

import basespecs.BaseUnitSpec
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{NotFoundException, UnauthorizedException}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class ExternalCaseManagementApiSpec extends BaseUnitSpec {

  val http = mock[VoaHttpClient]

  val connector = new ExternalCaseManagementApi(http, mock[ServicesConfig]) {
    override lazy val baseUrl: String = "http://some-uri"
    override lazy val voaModernisedApiStubBaseUrl: String = "http://some-uri"
  }

  "ExternalCaseManagementApi get check cases" should {
    "get agents check cases" in {

      val submissionId = "123"

      when(http.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any())).thenReturn(Future.successful(Some(AgentCheckCasesResponse(1, 15, 4, 4, List(
       AgentCheckCase(
         checkCaseSubmissionId = "CHK-1ZRPW9Q",
         checkCaseReference = "CHK100028783",
         checkCaseStatus = "OPEN",
         address = "Not Available",
         uarn = 7538840000L,
         createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
         settledDate = None,
         client = Client(
           organisationId = 123,
           organisationName = "ABC"
         ),
         submittedBy = "OA Ltd"
       ),
        AgentCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW99",
          checkCaseReference = "CHK100028784",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
          client = Client(
            organisationId = 123,
            organisationName = "ABC"
          ),
          submittedBy = "OA Ltd"
        ),
        AgentCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW5B",
          checkCaseReference = "CHK100028650",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
          client = Client(
            organisationId = 123,
            organisationName = "ABC"
          ),
          submittedBy = "OA Ltd"
        ),
        AgentCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW39",
          checkCaseReference = "CHK100028589",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
          client = Client(
            organisationId = 123,
            organisationName = "ABC"
          ),
          submittedBy = "OA Ltd"
        )
      )))))

      await(connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal)).get.filterTotal shouldBe 4
    }

    "handle 403 from get agents check cases" in {

      val submissionId = "123"

      when(http.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("unauthorised")))

      await(connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal)) shouldBe None
    }

    "handle 404 get agent check cases" in {

      val submissionId = "123"

      when(http.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("unauthorised")))

      await(connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal)) shouldBe None
    }

    "get client check cases" in {

      val submissionId = "123"

      when(http.GET[Option[OwnerCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(OwnerCheckCasesResponse(1, 15, 4, 4, List(
        OwnerCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW9Q",
          checkCaseReference = "CHK100028783",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
          agent = None,

          submittedBy = "OA Ltd"
        ),
          OwnerCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW99",
          checkCaseReference = "CHK100028784",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
            agent = None,

          submittedBy = "OA Ltd"
        ),
          OwnerCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW5B",
          checkCaseReference = "CHK100028650",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
            agent = None,
          submittedBy = "OA Ltd"
        ),
          OwnerCheckCase(
          checkCaseSubmissionId = "CHK-1ZRPW39",
          checkCaseReference = "CHK100028589",
          checkCaseStatus = "OPEN",
          address = "Not Available",
          uarn = 7538840000L,
          createdDateTime = LocalDateTime.parse("2018-07-31T08:16:54"),
          settledDate = None,
            agent = None,
          submittedBy = "OA Ltd"
        )
      )))))


      await(connector.getCheckCases(submissionId, "client")(hc, requestWithPrincipal)).get.filterTotal shouldBe 4
    }

    "handle 403 from get client check cases" in {

      val submissionId = "123"

      when(http.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("unauthorised")))

      await(connector.getCheckCases(submissionId, "client")(hc, requestWithPrincipal)) shouldBe None
    }

    "handle 404 get client check cases" in {

      val submissionId = "123"

      when(http.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new NotFoundException("unauthorised")))

      await(connector.getCheckCases(submissionId, "client")(hc, requestWithPrincipal)) shouldBe None
    }
  }
}
