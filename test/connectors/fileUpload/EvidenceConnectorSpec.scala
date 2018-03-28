/*
 * Copyright 2018 HM Revenue & Customs
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

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.{Source, StreamConverters}
import akka.util.ByteString
import com.codahale.metrics.{Meter, MetricRegistry}
import com.github.tomakehurst.wiremock.client.WireMock._
import com.kenshoo.play.metrics.Metrics
import connectors.{EvidenceConnector, WireMockSpec}
import helpers.{AnswerSugar, SimpleWsHttpTestApplication}
import infrastructure.SimpleWSHttp
import org.mockito.ArgumentMatchers.{any => mockitoAny}
import org.mockito.Mockito.{times, when}
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws.{WSRequest, WSResponse}
import play.api.mvc.MultipartFormData
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport
import uk.gov.hmrc.http.{HeaderCarrier, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class EvidenceConnectorSpec extends WireMockSpec with SimpleWsHttpTestApplication with MicroserviceFilterSupport
  with BeforeAndAfterEach with MockitoSugar with AnswerSugar {

  val metrics = mock[Metrics]
  val ws = mock[SimpleWSHttp]
  val http = mock[WSRequest]
  val registry = mock[MetricRegistry]
  val response = mock[WSResponse]
  val failResponse = mock[WSResponse]
  val meter = mock[Meter]

  override def beforeEach() = {
    super.beforeEach()
    Mockito.reset(response, failResponse, registry)
    when(ws.buildRequest(mockitoAny())(mockitoAny())).thenReturn(http)
    when(http.withHeaders(mockitoAny())).thenReturn(http)
    when(metrics.defaultRegistry).thenReturn(registry)
    when(registry.meter(mockitoAny())).thenReturn(meter)
  }

  "Evidence connector" should {
    "be able to upload a file to the old file submission endpoint using PUT" in {
      val metrics = mock[Metrics]
      val connector = new EvidenceConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
        override lazy val url = mockServerUrl
        override lazy val uploadEndpoint = "/customer-management-api/customer/evidence"
      }

      implicit val fakeHc = HeaderCarrier()
      val file = getClass.getResource("/document.pdf").getFile
      val metadata = EnvelopeMetadata("aSubmissionId", 12345)

      stubFor(put(urlEqualTo("/customer-management-api/customer/evidence"))
        .withRequestBody(containing(file))
        .withRequestBody(containing("aSubmissionId"))
        .withRequestBody(containing("12345"))
        .willReturn(aResponse().withStatus(200)))

      noException should be thrownBy await(connector.uploadFile("FileName", StreamConverters.fromInputStream { () => new ByteArrayInputStream(file.getBytes) }, metadata))
    }

    "be able to upload a file to the new file submission endpoint using POST" in {
      val metrics = mock[Metrics]
      val connector = new EvidenceConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
        override lazy val url = mockServerUrl
        override lazy val uploadEndpoint = "/case-documents-app-management-api/external/document"
      }

      implicit val fakeHc = HeaderCarrier()
      val file = getClass.getResource("/document.pdf").getFile
      val metadata = EnvelopeMetadata("aSubmissionId", 12345)

      stubFor(post(urlEqualTo("/case-documents-app-management-api/external/document"))
        .withRequestBody(containing(file))
        .withRequestBody(containing("aSubmissionId"))
        .withRequestBody(containing("12345"))
        .willReturn(aResponse().withStatus(200)))

      noException should be thrownBy await(connector.uploadFile("FileName", StreamConverters.fromInputStream { () => new ByteArrayInputStream(file.getBytes) }, metadata))
    }

    "URL Decode a filename with a non windows character in it replacing with a - (temporary fix) in the PUT body" in {
      val metrics = mock[Metrics]
      val connector = new EvidenceConnector(fakeApplication.injector.instanceOf[SimpleWSHttp], metrics) {
        override lazy val url = mockServerUrl
      }

      implicit val fakeHc = HeaderCarrier()
      val file = getClass.getResource("/document.pdf").getFile
      val metadata = EnvelopeMetadata("aSubmissionId", 12345)
      val filenames = Map(
        "file:name*.pdf" -> "file-name-.pdf",
        "sharpscanner:@:gmail?.com.pdf" -> "sharpscanner-@-gmail-.com.pdf",
        "Scan 15 Jun :2017, 13.04<>.pdf" -> "Scan 15 Jun -2017, 13.04--.pdf"
      )

      for ((encoded, decoded) <- filenames) {
        stubFor(put(urlEqualTo("/customer-management-api/customer/evidence"))
          .withRequestBody(containing(s"""filename="$decoded""""))
          .withRequestBody(containing("aSubmissionId"))
          .withRequestBody(containing("12345"))
          .willReturn(aResponse().withStatus(200)))

        noException should be thrownBy await(connector.uploadFile(encoded, StreamConverters.fromInputStream { () => new ByteArrayInputStream(file.getBytes) }, metadata))
      }
    }

    "Uploading a file to modernized" should {
      implicit val fakeHc = HeaderCarrier()

      val metadata = EnvelopeMetadata("aSubmissionId", 12345)
      val connector = new EvidenceConnector(ws, metrics)

      "map success status to modernized.upload.200" in {
        when(registry.meter(ArgumentMatchers.eq("modernized.upload.200"))).thenReturn(meter)
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.successful(response))
        when(response.status).thenReturn(200)

        await(connector.uploadFile("1", Source.empty, metadata))
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.200")))
      }

      "map 4xx failure to modernized.upload.4xx" in {
        when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
        when(failResponse.status).thenReturn(400)
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.successful(failResponse))

        intercept[Upstream4xxResponse] {
          await(connector.uploadFile("1", Source.empty, metadata))
        }
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.400")))
      }

      "map 5xx failure to modernized.upload.5xx" in {
        when(failResponse.header(ArgumentMatchers.eq("location"))).thenReturn(None)
        when(failResponse.status).thenReturn(502)
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.successful(failResponse))

        intercept[Upstream5xxResponse] {
          await(connector.uploadFile("1", Source.empty, metadata))
        }
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.502")))
      }

      "map Upstream5xx exceptions to modernized.upload.5xx" in {
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.failed(Upstream5xxResponse("", 502, 502)))

        intercept[Upstream5xxResponse] {
          await(connector.uploadFile("1", Source.empty, metadata))
        }
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.502")))
      }

      "map Upstream4xx exceptions to modernized.upload.4xx" in {
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.failed(Upstream4xxResponse("", 400, 400)))

        intercept[Upstream4xxResponse] {
          await(connector.uploadFile("1", Source.empty, metadata))
        }
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.400")))
      }

      "map Other exceptions to modernized.upload.[NAME]" in {
        when(http.put(mockitoAny(classOf[Source[MultipartFormData.Part[Source[ByteString, _]], _]]))).thenReturn(Future.failed(new NullPointerException()))

        intercept[NullPointerException] {
          await(connector.uploadFile("1", Source.empty, metadata))
        }
        eventually(Mockito.verify(registry, times(1)).meter(ArgumentMatchers.eq("modernized.upload.NullPointerException")))
      }
    }
  }
}
