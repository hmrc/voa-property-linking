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

package connectors.fileUpload

import java.util.UUID

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockSpec
import helpers.WithSimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.{verify => mockitoVerify, _}
import org.mockito.ArgumentMatchers.{any => mockitoAny, _}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.{StreamedResponse, WSClient, WSRequest}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class FileUploadSpec extends WireMockSpec with WithSimpleWsHttpTestApplication with MicroserviceFilterSupport with MockitoSugar {

  "FileUploadConnector" should {
    "be able to download files from the file upload service" in {
      implicit val fakeHc = HeaderCarrier()

      val fileBytes = getClass.getResource("/document.pdf").getFile.getBytes
      val fileUrl = s"/file-upload/envelopes/${java.util.UUID.randomUUID()}/files/document.pdf/content"
      val wsClient = fakeApplication.injector.instanceOf[WSClient]
      val http = fakeApplication.injector.instanceOf[SimpleWSHttp]
      val connector = new FileUploadConnector(wsClient, http) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(fileUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/octet-stream")
          .withBody(fileBytes)
        )
      )

      await(await(connector.downloadFile(fileUrl)).body.runFold("")(_ + _.decodeString("UTF-8"))) should be(new String(fileBytes))
    }

    "set the USER_AGENT header to the appName" in {
      implicit val fakeHc = HeaderCarrier()

      val mockWsClient = mock[WSClient]
      val mockHttp = mock[SimpleWSHttp]
      val mockWsRequest = mock[WSRequest]
      val mockFutureStreamedResponse = Future.successful(mock[StreamedResponse])

      val connector = new FileUploadConnector(mockWsClient, mockHttp) {
        override lazy val url = ""
      }

      when(mockWsClient.url(anyString())).thenReturn(mockWsRequest)
      when(mockWsRequest.withHeaders(mockitoAny())).thenReturn(mockWsRequest)
      when(mockWsRequest.withMethod(mockitoAny())).thenReturn(mockWsRequest)
      when(mockWsRequest.stream()).thenReturn(mockFutureStreamedResponse)

      await(connector.downloadFile("someFile.pdf"))

      mockitoVerify(mockWsRequest, times(1)).withHeaders("User-Agent" -> "voa-property-linking")
    }

    "convert envelope metadata into a valid payload" in {
      val connector = new FileUploadConnector(mock[WSClient], fakeApplication.injector.instanceOf[SimpleWSHttp]) {
        override lazy val url = mockServerUrl
      }

      val metadata = EnvelopeMetadata("aSubmission", 123)
      val fileId = UUID.randomUUID().toString

      stubFor(post(urlEqualTo("/file-upload/envelopes"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("location", s"/file-upload/envelopes/$fileId")))

      val expectedJson = Json.stringify(Json.obj(
        "callbackUrl" -> "/some-callback",
        "metadata" -> Json.obj(
          "submissionId" -> "aSubmission",
          "personId" -> 123
        ),
        "constraints" -> Json.obj(
          "maxItems" -> 1,
          "maxSize" -> "10MB",
          "contentTypes" -> Json.arr("application/pdf", "image/jpeg")
        )
      ))


      await(connector.createEnvelope(metadata, "/some-callback")(HeaderCarrier()))

      verify(postRequestedFor(urlEqualTo("/file-upload/envelopes")).withRequestBody(equalToJson(expectedJson)))
    }

    "extract and return the envelope ID from the response headers" in {
      val connector = new FileUploadConnector(mock[WSClient], fakeApplication.injector.instanceOf[SimpleWSHttp]) {
        override lazy val url = mockServerUrl
      }

      val metadata = EnvelopeMetadata("aSubmission", 123)
      val fileId = UUID.randomUUID().toString

      stubFor(post(urlEqualTo("/file-upload/envelopes"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("location", s"/file-upload/envelopes/$fileId")))

      val res = await(connector.createEnvelope(metadata, "/some-callback")(HeaderCarrier()))

      res shouldBe Some(fileId)
    }
  }
}
