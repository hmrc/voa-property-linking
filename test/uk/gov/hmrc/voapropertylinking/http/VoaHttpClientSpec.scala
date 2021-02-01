/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.http

import basespecs.BaseUnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.logging._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.voapropertylinking.auth.Principal

class VoaHttpClientSpec extends BaseUnitSpec {

  val authorization = Authorization("authorization")
  val forwarded = ForwardedFor("ipAdress")
  val sessionId = SessionId("1234567890")
  val requestId = RequestId("0987654321")
  val deviceId = "testDeviceId"
  val akamaiReputation = AkamaiReputation("foo")

  trait Setup {

    val mockUrl = "http://mock-url"

    val mockQueryParams = Seq("key" -> "value")
    val mockHeaders = Seq("key"     -> "value")
    val mockHttpClient: DefaultHttpClient = mock[DefaultHttpClient]
    val headerCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])

    val voaHttpClient = new VoaHttpClient(mockHttpClient)
  }

  "using the VOA HTTP Client" should {

    def checkGovernmentGatewayHeaders(headerCaptor: ArgumentCaptor[HeaderCarrier])(implicit principal: Principal) =
      headerCaptor.getValue.extraHeaders shouldBe Seq(
        "GG-EXTERNAL-ID" -> principal.externalId,
        "GG-GROUP-ID"    -> principal.groupId
      )

    "preserve the existing headers when adding the extra GG headers" in new Setup {
      val voaHc: HeaderCarrier = voaHttpClient.buildHeaderCarrier(hc, principal)

      voaHc shouldBe hc.withExtraHeaders(
        Seq(
          "GG-EXTERNAL-ID" -> principal.externalId,
          "GG-GROUP-ID"    -> principal.groupId
        ): _*)
    }

    "enrich the GG headers when calling a GET" in new Setup {
      voaHttpClient.GET[HttpResponse](mockUrl)

      verify(mockHttpClient)
        .GET(ArgumentMatchers.eq(mockUrl))(any(), headerCaptor.capture(), any())

      checkGovernmentGatewayHeaders(headerCaptor)
    }

    "enrich the GG headers when calling a GET with query params" in new Setup {
      voaHttpClient.GET[HttpResponse](mockUrl, mockQueryParams)

      verify(mockHttpClient)
        .GET(ArgumentMatchers.eq(mockUrl), ArgumentMatchers.eq(mockQueryParams))(any(), headerCaptor.capture(), any())

      checkGovernmentGatewayHeaders(headerCaptor)
    }

    "enrich the GG headers when calling a DELETE" in new Setup {
      voaHttpClient.DELETE[HttpResponse](mockUrl)

      verify(mockHttpClient)
        .DELETE(ArgumentMatchers.eq(mockUrl), any())(any(), headerCaptor.capture(), any())

      checkGovernmentGatewayHeaders(headerCaptor)
    }

    "enrich the GG headers when calling a PUT" in new Setup {
      voaHttpClient.PUT[String, HttpResponse](mockUrl, "")

      verify(mockHttpClient)
        .PUT(ArgumentMatchers.eq(mockUrl), ArgumentMatchers.eq(""), any())(any(), any(), headerCaptor.capture(), any())

      checkGovernmentGatewayHeaders(headerCaptor)
    }

    "enrich the GG headers when calling a POST" in new Setup {
      voaHttpClient.POST[String, HttpResponse](mockUrl, "", mockHeaders)

      verify(mockHttpClient)
        .POST(ArgumentMatchers.eq(mockUrl), ArgumentMatchers.eq(""), ArgumentMatchers.eq(mockHeaders))(
          any(),
          any(),
          headerCaptor.capture(),
          any())

      checkGovernmentGatewayHeaders(headerCaptor)
    }
  }

}
