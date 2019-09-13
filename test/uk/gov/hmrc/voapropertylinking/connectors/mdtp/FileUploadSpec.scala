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

package uk.gov.hmrc.voapropertylinking.connectors.mdtp

import java.util.UUID

import basespecs.WireMockSpec
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.concurrent.Eventually._
import play.api.libs.json.Json
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class FileUploadSpec extends WireMockSpec with SimpleWsHttpTestApplication {

  implicit lazy val mat = fakeApplication.materializer

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockMetrics.defaultRegistry).thenReturn(mockMetricRegistry)
    when(mockSimpleWSHttp.buildRequest(anyString())(any())).thenReturn(mockWSRequest)
    when(mockWSRequest.withMethod(anyString())).thenReturn(mockWSRequest)
    when(mockWSRequest.stream()).thenReturn(Future.successful(mockStreamedResponse))
    when(mockStreamedResponse.headers).thenReturn(mockWSResponseHeaders)
    when(mockMetricRegistry.meter(any())).thenReturn(mockMeter)
  }

  "FileUploadConnector" should {
    "be able to download files from the file upload service" in {

      val fileBytes = getClass.getResource("/document.pdf").getFile.getBytes
      val fileUrl = s"/file-upload/envelopes/${java.util.UUID.randomUUID()}/files/document.pdf/content"
      val connector = new FileUploadConnector(
        fakeApplication.injector.instanceOf[SimpleWSHttp],
        mockMetrics,
        fakeApplication.injector.instanceOf[ServicesConfig]
      ) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(fileUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/octet-stream")
          .withBody(fileBytes)
        )
      )

      connector.downloadFile(fileUrl).futureValue.body.runFold("")(_ + _.decodeString("UTF-8")).futureValue should be(new String(fileBytes))
    }

    "handle 404 responses when retrieving envelope details from FUaaS" in {
      val envelopeId = UUID.randomUUID().toString

      val http = fakeApplication.injector.instanceOf[SimpleWSHttp]

      val connector = new FileUploadConnector(
        http,
        mockMetrics,
        fakeApplication.injector.instanceOf[ServicesConfig]
      ) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(s"/file-upload/envelopes/$envelopeId")).willReturn(aResponse().withStatus(404)))

      connector.getEnvelopeDetails(envelopeId)(HeaderCarrier()).futureValue shouldBe EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 0))
    }

    "convert envelope metadata into a valid payload" in {
      val connector = new FileUploadConnector(
        fakeApplication.injector.instanceOf[SimpleWSHttp],
        mockMetrics,
        fakeApplication.injector.instanceOf[ServicesConfig]
      ) {
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
          "contentTypes" -> Json.arr("application/pdf", "image/jpeg"),
          "allowZeroLengthFiles" -> false
        )
      ))


      connector.createEnvelope(metadata, "/some-callback")(HeaderCarrier()).futureValue

      WireMock.verify(postRequestedFor(urlEqualTo("/file-upload/envelopes")).withRequestBody(equalToJson(expectedJson)))
    }

    "extract and return the envelope ID from the response headers" in {
      val connector = new FileUploadConnector(
        fakeApplication.injector.instanceOf[SimpleWSHttp],
        mockMetrics,
        fakeApplication.injector.instanceOf[ServicesConfig]
      ) {
        override lazy val url = mockServerUrl
      }

      val metadata = EnvelopeMetadata("aSubmission", 123)
      val fileId = UUID.randomUUID().toString

      stubFor(post(urlEqualTo("/file-upload/envelopes"))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("location", s"/file-upload/envelopes/$fileId")))

      val res = connector.createEnvelope(metadata, "/some-callback")(HeaderCarrier()).futureValue

      res shouldBe Some(fileId)
    }

    "ignore errors when deleting envelopes from FUaaS" in {
      val envelopeId = UUID.randomUUID().toString
      val connector = new FileUploadConnector(
        fakeApplication.injector.instanceOf[SimpleWSHttp],
        mockMetrics,
        fakeApplication.injector.instanceOf[ServicesConfig]
      ) {
        override lazy val url = mockServerUrl
      }

      stubFor(delete(urlEqualTo(s"/file-upload/envelopes/$envelopeId")).willReturn(aResponse().withStatus(404)))

      noException shouldBe thrownBy(connector.deleteEnvelope(envelopeId)(HeaderCarrier()).futureValue)
    }

    "send the correct classification of metric" when {
      "creating an envelope's status" should {
        "map success to file-upload.envelope.create.201" in {
          when(mockHttpResponse.header(ArgumentMatchers.eq("location"))).thenReturn(Some("MATCH"))
          when(mockHttpResponse.status).thenReturn(201)
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.successful(mockHttpResponse))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.201")))
        }

        "map 4xx failure to file-upload.envelope.create.4xx" in {
          when(mockHttpResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(mockHttpResponse.status).thenReturn(400)
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.successful(mockHttpResponse))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.400")))
        }

        "map 5xx failure to file-upload.envelope.create.5xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockHttpResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(mockHttpResponse.status).thenReturn(502)
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.successful(mockHttpResponse))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.502")))
        }

        "map Upstream5xx exceptions to file-upload.envelope.create.5xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.502")))
        }

        "map Upstream4xx exceptions to file-upload.envelope.create.4xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.400")))
        }

        "map Other exceptions to file-upload.envelope.create.[NAME]" in {
          when(mockSimpleWSHttp.POST[CreateEnvelopePayload, HttpResponse](anyString(), any(), any())(any(), any(), any(), any()))
            .thenReturn(Future.failed(new NullPointerException()))

          fakeFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()).futureValue

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.create.NullPointerException")))
        }
      }

      "deleting an envelope" should {
        "map success status to file-upload.envelope.delete.200" in {
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))
          when(mockHttpResponse.status).thenReturn(200)

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.200")))
        }

        "map 4xx failure to file-upload.envelope.delete.4xx" in {
          when(mockHttpResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(mockHttpResponse.status).thenReturn(400)
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any())).thenReturn(Future.successful(mockHttpResponse))

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.400")))
        }

        "map 5xx failure to file-upload.envelope.delete.5xx" in {
          when(mockHttpResponse.header(ArgumentMatchers.eq("location")))
            .thenReturn(None)
          when(mockHttpResponse.status)
            .thenReturn(502)
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.successful(mockHttpResponse))

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.502")))
        }

        "map Upstream5xx exceptions to file-upload.envelope.delete.5xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.502")))
        }

        "map Upstream4xx exceptions to file-upload.envelope.create.4xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.400")))
        }

        "map Other exceptions to file-upload.envelope.delete.[NAME]" in {
          when(mockSimpleWSHttp.DELETE[HttpResponse](anyString())(any(), any(), any()))
            .thenReturn(Future.failed(new NullPointerException()))

          fakeFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.envelope.delete.NullPointerException")))
        }
      }

      "downloading a file" should {
        "map success status to file-upload.download.200" in {
          when(mockWSResponseHeaders.status).thenReturn(200)

          fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.200")))
        }

        "map 4xx failure to file-upload.download.4xx" in {
          when(mockWSResponseHeaders.status).thenReturn(400)

          fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.400")))
        }

        "map 5xx failure to file-upload.download.5xx" in {
          when(mockWSResponseHeaders.status).thenReturn(500)

          fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).futureValue
          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.500")))
        }

        "map Upstream5xx exceptions to file-upload.download.5xx" in {
          when(mockWSRequest.stream())
            .thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          whenReady(
            fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).failed
          )(_ shouldBe an[Upstream5xxResponse])

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.502")))
        }

        "map Upstream4xx exceptions to file-upload.download.4xx" in {
          Mockito.reset(mockMetricRegistry)
          when(mockWSRequest.stream())
            .thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          whenReady(
            fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).failed
          )(_ shouldBe an[Upstream4xxResponse])

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.400")))
        }

        "map Other exceptions to file-upload.download.[NAME]" in {
          when(mockWSRequest.stream())
            .thenReturn(Future.failed(new NullPointerException()))

          whenReady(
            fakeFileUploadConnector.downloadFile("1")(HeaderCarrier()).failed
          )(_ shouldBe a[NullPointerException])

          eventually(Mockito.verify(mockMetricRegistry).meter(ArgumentMatchers.eq("file-upload.download.NullPointerException")))
        }

      }
    }
  }

  object fakeFileUploadConnector extends FileUploadConnector(
    http = mockSimpleWSHttp,
    metrics = mockMetrics,
    config = fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val url = ""
  }

}
