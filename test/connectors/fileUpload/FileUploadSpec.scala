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

import com.codahale.metrics.{Meter, MetricRegistry}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.kenshoo.play.metrics.Metrics
import connectors.WireMockSpec
import helpers.SimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import org.mockito.ArgumentMatchers.{any => mockitoAny, _}
import org.mockito.Mockito.{verify => mockitoVerify, _}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually._
import org.scalatest.mock.MockitoSugar
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.{StreamedResponse, WSRequest, WSResponseHeaders}
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse, Upstream4xxResponse, Upstream5xxResponse}

import scala.concurrent.Future

class FileUploadSpec extends WireMockSpec with SimpleWsHttpTestApplication with MicroserviceFilterSupport with MockitoSugar with BeforeAndAfterEach {

  val http = mock[SimpleWSHttp]
  val metrics = mock[Metrics]
  val registry = mock[MetricRegistry]
  val response = mock[HttpResponse]
  val failResponse = mock[HttpResponse]
  val meter = mock[Meter]
  val request = mock[WSRequest]
  val strResponse = mock[StreamedResponse]
  val headers = mock[WSResponseHeaders]

  override def beforeEach(): Unit = {
    super.beforeEach()
    Mockito.reset(http, response, metrics, registry, strResponse, headers)
    when(metrics.defaultRegistry).thenReturn(registry)
    when(http.buildRequest(anyString())(mockitoAny())).thenReturn(request)
    when(request.withMethod(anyString())).thenReturn(request)
    when(request.stream()).thenReturn(Future.successful(strResponse))
    when(strResponse.headers).thenReturn(headers)
    when(registry.meter(mockitoAny())).thenReturn(meter)
  }

  "FileUploadConnector" should {
    "be able to download files from the file upload service" in {
      implicit val fakeHc = HeaderCarrier()

      val fileBytes = getClass.getResource("/document.pdf").getFile.getBytes
      val fileUrl = s"/file-upload/envelopes/${java.util.UUID.randomUUID()}/files/document.pdf/content"
      val connector = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
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

    "handle 404 responses when retrieving envelope details from FUaaS" in {
      val envelopeId = UUID.randomUUID().toString

      val http = fakeApplication.injector.instanceOf[SimpleWSHttp]

      val connector = new FileUploadConnector(http, metrics) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(s"/file-upload/envelopes/$envelopeId")).willReturn(aResponse().withStatus(404)))

      await(connector.getEnvelopeDetails(envelopeId)(HeaderCarrier())) shouldBe EnvelopeInfo(envelopeId, "NOT_EXISTING", Nil, EnvelopeMetadata("nosubmissionid", 0))
    }

    "handle other error codes when retrieving envelope details from FUaaS" in {
      val envelopeId = UUID.randomUUID().toString
      val connector = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(s"/file-upload/envelopes/$envelopeId")).willReturn(aResponse().withStatus(502)))

      await(connector.getEnvelopeDetails(envelopeId)(HeaderCarrier())) shouldBe EnvelopeInfo(envelopeId, "UNKNOWN_ERROR", Nil, EnvelopeMetadata("nosubmissionid", 0))
    }

    "convert envelope metadata into a valid payload" in {
      val connector = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
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
      val connector = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
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

    "ignore errors when deleting envelopes from FUaaS" in {
      val envelopeId = UUID.randomUUID().toString
      val connector = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
        override lazy val url = mockServerUrl
      }

      stubFor(delete(urlEqualTo(s"/file-upload/envelopes/$envelopeId")).willReturn(aResponse().withStatus(404)))

      noException shouldBe thrownBy(await(connector.deleteEnvelope(envelopeId)(HeaderCarrier())))
    }

    "send the correct classification of metric" when {
      "creating an envelope's status" should {
        "map success to file-upload.envelope.create.201" in {
          when(response.header(ArgumentMatchers.eq("location"))).thenReturn(Some("MATCH"))
          when(response.status).thenReturn(201)
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.successful(response))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.201")))
        }

        "map 4xx failure to file-upload.envelope.create.4xx" in {
          when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(failResponse.status).thenReturn(400)
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.successful(failResponse))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.400")))
        }

        "map 5xx failure to file-upload.envelope.create.5xx" in {
          when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(failResponse.status).thenReturn(502)
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.successful(failResponse))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.502")))
        }

        "map Upstream5xx exceptions to file-upload.envelope.create.5xx" in {
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.502")))
        }

        "map Upstream4xx exceptions to file-upload.envelope.create.4xx" in {
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.400")))
        }

        "map Other exceptions to file-upload.envelope.create.[NAME]" in {
          when(http.POST[CreateEnvelopePayload, HttpResponse](anyString(), mockitoAny(), mockitoAny())(mockitoAny(), mockitoAny(), mockitoAny())).thenReturn(Future.failed(new NullPointerException()))

          await(mockFileUploadConnector.createEnvelope(EnvelopeMetadata("subId", 1L), "")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.create.NullPointerException")))
        }
      }

      "deleting an envelope" should {
        "map success status to file-upload.envelope.delete.200" in {
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.successful(response))
          when(response.status).thenReturn(200)

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.200")))
        }

        "map 4xx failure to file-upload.envelope.delete.4xx" in {
          when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(failResponse.status).thenReturn(400)
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.successful(failResponse))

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.400")))
        }

        "map 5xx failure to file-upload.envelope.delete.5xx" in {
          when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
          when(failResponse.status).thenReturn(502)
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.successful(failResponse))

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.502")))
        }

        "map Upstream5xx exceptions to file-upload.envelope.delete.5xx" in {
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.502")))
        }

        "map Upstream4xx exceptions to file-upload.envelope.create.4xx" in {
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.400")))
        }

        "map Other exceptions to file-upload.envelope.delete.[NAME]" in {
          when(http.DELETE[HttpResponse](anyString())(mockitoAny(), mockitoAny())).thenReturn(Future.failed(new NullPointerException()))

          await(mockFileUploadConnector.deleteEnvelope("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.envelope.delete.NullPointerException")))
        }
      }

      "downloading a file" should {
        "map success status to file-upload.download.200" in {
          when(headers.status).thenReturn(200)

          await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.200")))
        }

        "map 4xx failure to file-upload.download.4xx" in {
          when(headers.status).thenReturn(400)

          await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.400")))
        }

        "map 5xx failure to file-upload.download.5xx" in {
          when(headers.status).thenReturn(500)

          await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.500")))
        }

        "map Upstream5xx exceptions to file-upload.download.5xx" in {
          when(request.stream()).thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

          intercept[Upstream5xxResponse] {
            await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          }

          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.502")))
        }

        "map Upstream4xx exceptions to file-upload.download.4xx" in {
          when(request.stream()).thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

          intercept[Upstream4xxResponse] {
            await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          }

          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.400")))
        }

        "map Other exceptions to file-upload.download.[NAME]" in {
          when(request.stream()).thenReturn(Future.failed(new NullPointerException()))

          intercept[NullPointerException] {
            await(mockFileUploadConnector.downloadFile("1")(HeaderCarrier()))
          }

          eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("file-upload.download.NullPointerException")))
        }

      }
    }
  }

  object mockFileUploadConnector extends FileUploadConnector(http, metrics) { override lazy val url = "" }
}
