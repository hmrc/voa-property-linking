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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import basespecs.BaseUnitSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myclients.{CheckCaseWithClient, CheckCasesWithClient}
import uk.gov.hmrc.voapropertylinking.models.modernised.casemanagement.check.myorganisation.{CheckCaseWithAgent, CheckCasesWithAgent}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class ModernisedExternalCaseManagementApiSpec extends BaseUnitSpec {

  val connector: ModernisedExternalCaseManagementApi =
    new ModernisedExternalCaseManagementApi(mockVoaHttpClient, mockAppConfig)

  trait Setup {
    val submissionId: String = "PL123AB"
    val checkCaseRef: String = "CHK123ABC"
    val valuationId: Long = 123456L
    when(mockAppConfig.proxyEnabled).thenReturn(false)
    when(mockAppConfig.apimSubscriptionKeyValue).thenReturn("subscriptionId")
    when(mockAppConfig.voaApiBaseUrl).thenReturn("http://some/url/voa")
    when(mockServicesConfig.baseUrl(any())).thenReturn("http://localhost:9949/")
  }

  "get my organisation check cases" when {
    "check cases exist under a property link" should {
      "return the my organisation check case response" in new Setup {
        val mockCheckCase: CheckCaseWithAgent = mock[CheckCaseWithAgent]

        when(mockVoaHttpClient.getWithGGHeaders[CheckCasesWithAgent](any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(CheckCasesWithAgent(1, 15, 4, 4, List(mockCheckCase))))

        val result: CheckCasesWithAgent =
          connector.getMyOrganisationCheckCases(submissionId)(requestWithPrincipal).futureValue

        result.start shouldBe 1
        result.size shouldBe 15
        result.total shouldBe 4
        result.filterTotal shouldBe 4
        result.checkCases.loneElement shouldBe mockCheckCase
      }
    }
  }

  "get my clients check cases" when {
    "check cases exist under a property link" should {
      "return the my clients check case response" in new Setup {
        val mockCheckCase: CheckCaseWithClient = mock[CheckCaseWithClient]

        when(mockVoaHttpClient.getWithGGHeaders[CheckCasesWithClient](any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(CheckCasesWithClient(1, 15, 4, 4, List(mockCheckCase))))

        val result: CheckCasesWithClient =
          connector.getMyClientsCheckCases(submissionId)(requestWithPrincipal).futureValue

        result.start shouldBe 1
        result.size shouldBe 15
        result.total shouldBe 4
        result.filterTotal shouldBe 4
        result.checkCases.loneElement shouldBe mockCheckCase
      }
    }
  }

  "canChallenge" should {

    trait CanChallangeSetup extends Setup {
      when(mockHttpResponse.status).thenReturn(200)
      when(mockHttpResponse.body).thenReturn("""{"result": true}""")
      when(mockVoaHttpClient.getWithGGHeaders[HttpResponse](any())(any(), any(), any(), any()))
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
          inside(Try(connector.canChallenge(submissionId, checkCaseRef, valuationId, "foobar"))) { case Failure(e) =>
            e shouldBe an[IllegalArgumentException]
            e.getMessage shouldBe "Unknown party foobar"
          }
        }
      }
    }
  }
}
