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

package uk.gov.hmrc.voapropertylinking.http

import basespecs.BaseUnitSpec
import izumi.reflect.Tag
import models.modernised.externalpropertylink.requests.CreatePropertyLink
import models.searchApi.Agent
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.BodyWritable
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.voapropertylinking.auth.Principal

import java.net.URL
import scala.concurrent.{ExecutionContext, Future}

class VoaHttpClientSpec extends BaseUnitSpec {

  val mockBody: CreatePropertyLink = mock[CreatePropertyLink]
  val authorization: Authorization = Authorization("authorization")
  val forwarded: ForwardedFor = ForwardedFor("ipAdress")
  val sessionId: SessionId = SessionId("1234567890")
  val requestId: RequestId = RequestId("0987654321")
  val deviceId = "testDeviceId"
  val akamaiReputation: AkamaiReputation = AkamaiReputation("foo")

  trait Setup {

    val mockUrl = new URL("http://mock-url")
    val withBodyMocks: Boolean = false

    val mockQueryParams: Seq[(String, String)] = Seq("key" -> "value")
    val mockHeaders: Seq[(String, String)] = Seq("key" -> "value")
    val mockHttpClient: HttpClientV2 = mock[HttpClientV2]
    val headerCaptor: ArgumentCaptor[HeaderCarrier] = ArgumentCaptor.forClass(classOf[HeaderCarrier])
    val headersCaptor: ArgumentCaptor[Seq[(String, String)]] = ArgumentCaptor.forClass(classOf[Seq[(String, String)]])

    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
    val mockRequestBuilderWithHeaders: RequestBuilder = mock[RequestBuilder]
    val mockRequestBuilderWithAdditionalHeaders: RequestBuilder = mock[RequestBuilder]
    val mockRequestBuilderWithBody: RequestBuilder = mock[RequestBuilder]

    val mockRequestBuilderWithProxy: RequestBuilder = mock[RequestBuilder]
    val voaHttpClient = new VoaHttpClient(mockHttpClient, mockAppConfig)

    when(mockAppConfig.apimSubscriptionKeyValue).thenReturn("dummy-key")
  }

  "using the VOA HTTP Client" should {

    def checkGovernmentGatewayHeaders(headers: Map[String, String])(implicit principal: Principal): Unit = {
      headers("GG-EXTERNAL-ID") shouldBe principal.externalId
      headers("GG-GROUP-ID") shouldBe principal.groupId
      headers should contain key "Ocp-Apim-Subscription-Key"
    }

    "preserve the existing headers when adding the extra GG headers" in new Setup {
      val voaHc: Seq[(String, String)] = voaHttpClient.buildHeadersWithGG(principal)
      checkGovernmentGatewayHeaders(voaHc.toMap)
    }

    "enrich the GG headers when calling a GET" in new Setup {
      when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilderWithHeaders)
      when(mockRequestBuilderWithHeaders.withProxy).thenReturn(mockRequestBuilderWithProxy)
      when(mockRequestBuilderWithProxy.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val captor = ArgumentCaptor.forClass(classOf[List[(String, String)]])

      voaHttpClient.getWithGGHeaders[HttpResponse](mockUrl.toString)

      verify(mockRequestBuilder).setHeader(captor.capture(): _*)

      val capturedHeaders: List[(String, String)] = captor.getValue

      capturedHeaders should contain("GG-EXTERNAL-ID" -> "external-id")
      capturedHeaders should contain("GG-GROUP-ID" -> "group-id")
      capturedHeaders should contain("Ocp-Apim-Subscription-Key" -> "dummy-key")
    }

    "enrich the GG headers when calling a DELETE" in new Setup {
      when(mockHttpClient.delete(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilderWithHeaders)
      when(mockRequestBuilderWithHeaders.setHeader(any())).thenReturn(mockRequestBuilderWithAdditionalHeaders)
      when(mockRequestBuilderWithAdditionalHeaders.withProxy).thenReturn(mockRequestBuilderWithProxy)
      when(mockRequestBuilderWithProxy.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))
      val captor = ArgumentCaptor.forClass(classOf[List[(String, String)]])

      voaHttpClient.deleteWithGgHeaders[HttpResponse](mockUrl.toString)

      verify(mockRequestBuilder).setHeader(captor.capture(): _*)

      val capturedHeaders: List[(String, String)] = captor.getValue

      capturedHeaders should contain("GG-EXTERNAL-ID" -> "external-id")
      capturedHeaders should contain("GG-GROUP-ID" -> "group-id")
      capturedHeaders should contain("Ocp-Apim-Subscription-Key" -> "dummy-key")
    }

    "enrich the GG headers when calling a PUT" in new Setup {
      val exampleBody: Agent = Agent(
        ref = 123L,
        name = "test org"
      )
      val jsBody: JsObject = Json.toJsObject(exampleBody)

      when(mockHttpClient.put(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilderWithHeaders)

      when(
        mockRequestBuilderWithHeaders.withBody(
          any[JsObject]
        )(
          any[BodyWritable[JsObject]],
          any[Tag[JsObject]],
          any[ExecutionContext]
        )
      ).thenAnswer(_ => mockRequestBuilderWithBody)

      when(mockRequestBuilderWithBody.withProxy).thenReturn(mockRequestBuilderWithProxy)
      when(mockRequestBuilderWithProxy.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      override val withBodyMocks: Boolean = true

      val captor = ArgumentCaptor.forClass(classOf[List[(String, String)]])

      voaHttpClient.putWithGgHeaders[HttpResponse](mockUrl.toString, jsBody)

      verify(mockRequestBuilder).setHeader(captor.capture(): _*)

      val capturedHeaders: List[(String, String)] = captor.getValue

      capturedHeaders should contain("GG-EXTERNAL-ID" -> "external-id")
      capturedHeaders should contain("GG-GROUP-ID" -> "group-id")
      capturedHeaders should contain("Ocp-Apim-Subscription-Key" -> "dummy-key")
    }

    "enrich the GG headers when calling a POST" in new Setup {
      val exampleBody: Agent = Agent(ref = 123L, name = "test org")

      override val withBodyMocks: Boolean = true

      when(mockHttpClient.post(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilderWithHeaders)
      when(
        mockRequestBuilderWithHeaders.withBody(any[JsObject])(
          any[BodyWritable[JsObject]],
          any[Tag[JsObject]],
          any[ExecutionContext]
        )
      ).thenAnswer(_ => mockRequestBuilderWithBody)

      when(mockRequestBuilderWithBody.withProxy).thenReturn(mockRequestBuilderWithProxy)
      when(mockRequestBuilderWithProxy.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val captor = ArgumentCaptor.forClass(classOf[List[(String, String)]])

      voaHttpClient.postWithGgHeaders[HttpResponse](mockUrl.toString, Json.toJsObject(exampleBody))

      verify(mockRequestBuilder).setHeader(captor.capture(): _*)

      val capturedHeaders: List[(String, String)] = captor.getValue

      capturedHeaders should contain("GG-EXTERNAL-ID" -> "external-id")
      capturedHeaders should contain("GG-GROUP-ID" -> "group-id")
      capturedHeaders should contain("Ocp-Apim-Subscription-Key" -> "dummy-key")
    }
  }
}
