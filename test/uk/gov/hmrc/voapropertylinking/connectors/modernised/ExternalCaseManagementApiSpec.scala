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
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.{HttpResponse, UnauthorizedException}
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import scala.util.{Failure, Try}

class ExternalCaseManagementApiSpec extends BaseUnitSpec {

  val connector: ExternalCaseManagementApi =
    new ExternalCaseManagementApi(mockVoaHttpClient, mockServicesConfig) {
      override lazy val voaModernisedApiStubBaseUrl: String = "http://some-uri"
    }

  trait Setup {
    val submissionId: String = "PL123AB"
    val checkCaseRef: String = "CHK123ABC"
    val valuationId: Long = 123456L
  }

  trait AgentSetup extends Setup {
    val agentCheckCases = List(
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
      )
    )
  }

  trait OwnerSetup extends Setup {
    val ownerCheckCases = List(
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
      )
    )
  }

  "ExternalCaseManagementApi get check cases" should {
    "get agents check cases" in new AgentSetup {
      when(mockVoaHttpClient.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(AgentCheckCasesResponse(1, 15, 4, 4, agentCheckCases))))

      connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal).futureValue.get.filterTotal shouldBe 4
    }

    "handle 403 from get agents check cases" in new AgentSetup {
      when(mockVoaHttpClient.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("unauthorised")))

      connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal).futureValue shouldBe None
    }

    "handle 404 get agent check cases" in new AgentSetup {
      when(mockVoaHttpClient.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      connector.getCheckCases(submissionId, "agent")(hc, requestWithPrincipal).futureValue shouldBe None
    }


    //this is really not a great solution but it's what I found
    "return nothing when party String is not recognised" in new AgentSetup {
      connector.getCheckCases(submissionId, "foobar")(hc, requestWithPrincipal).futureValue shouldBe None
    }

    "get client check cases" in new OwnerSetup {
      when(mockVoaHttpClient.GET[Option[OwnerCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Some(OwnerCheckCasesResponse(1, 15, 4, 4, ownerCheckCases))))

      connector.getCheckCases(submissionId, "client")(hc, requestWithPrincipal).futureValue.get.filterTotal shouldBe 4
    }

    "handle 403 from get client check cases" in new OwnerSetup {
      when(mockVoaHttpClient.GET[Option[AgentCheckCasesResponse]](any())(any(), any(), any(), any()))
        .thenReturn(Future.failed(new UnauthorizedException("unauthorised")))

      connector.getCheckCases(submissionId, "client")(hc, requestWithPrincipal).futureValue shouldBe None
    }

    "handle 404 get client check cases" in new OwnerSetup {
      connector.getCheckCases(submissionId, "foobar")(hc, requestWithPrincipal).futureValue shouldBe None
    }
  }

  "canChallenge" should {

    trait CanChallangeSetup extends Setup {
      when(mockHttpResponse.status).thenReturn(200)
      when(mockHttpResponse.body).thenReturn("""{"result": true}""")
      when(mockVoaHttpClient.GET[HttpResponse](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(mockHttpResponse))
    }

    "return a successful CanChallengeResponse" when {
      "client party calling" in new CanChallangeSetup {
        inside(connector.canChallenge(submissionId, checkCaseRef, valuationId, "client").futureValue) {
          case Some(CanChallengeResponse(result, _, _)) => result shouldBe true
        }
      }
      "agent party calling" in new CanChallangeSetup {
        inside(connector.canChallenge(submissionId, checkCaseRef, valuationId, "agent").futureValue) {
          case Some(CanChallengeResponse(result, _, _)) => result shouldBe true
        }
      }

      "return nothing" when {
        "call to modernised returns non-200 status code" in new CanChallangeSetup {
          when(mockHttpResponse.status).thenReturn(400)
          connector.canChallenge(submissionId, checkCaseRef, valuationId, "client").futureValue shouldBe None
        }
      }

      "error with an exception" when {
        "unrecognised party" in new CanChallangeSetup {
          inside(Try(connector.canChallenge(submissionId, checkCaseRef, valuationId, "foobar"))) {
            case Failure(e) =>
              e shouldBe an[IllegalArgumentException]
              e.getMessage shouldBe "Unknown party foobar"
          }
        }
      }
    }
  }
}
