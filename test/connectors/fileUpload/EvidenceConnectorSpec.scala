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

import java.io.ByteArrayInputStream

import akka.stream.scaladsl.StreamConverters
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.{EvidenceConnector, WireMockSpec}
import helpers.{AnswerSugar, WithSimpleWsHttpTestApplication}
import org.scalatest.mock.MockitoSugar
import play.api.libs.ws.ahc.AhcWSClient
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.HeaderCarrier

class EvidenceConnectorSpec extends WireMockSpec with WithSimpleWsHttpTestApplication with MicroserviceFilterSupport
  with MockitoSugar with AnswerSugar {
  "Evidence connector" should {
    "be able to upload a file" in {
      val connector = new EvidenceConnector(AhcWSClient()) {
        override lazy val url = mockServerUrl
      }

      implicit val fakeHc = HeaderCarrier()
      val file = getClass.getResource("/document.pdf").getFile
      val metadata = EnvelopeMetadata("aSubmissionId", 12345)

      stubFor(put(urlEqualTo("/customer-management-api/customer/evidence"))
        .withHeader("Ocp-Apim-Subscription-Key", matching(fakeApplication.configuration.getString("voaApi.subscriptionKeyHeader").getOrElse("")))
        .withHeader("Ocp-Apim-Trace", matching(fakeApplication.configuration.getString("voaApi.traceHeader").getOrElse("")))
        .withRequestBody(containing(file))
        .withRequestBody(containing("aSubmissionId"))
        .withRequestBody(containing("12345"))
        .willReturn(aResponse().withStatus(200)))

      noException should be thrownBy await(connector.uploadFile("FileName", StreamConverters.fromInputStream { () => new ByteArrayInputStream(file.getBytes) }, metadata))
    }

    "Properly URL Decode a filename in the PUT body" in {
      val connector = new EvidenceConnector(AhcWSClient()) {
        override lazy val url = mockServerUrl
      }

      implicit val fakeHc = HeaderCarrier()
      val file = getClass.getResource("/document.pdf").getFile
      val metadata = EnvelopeMetadata("aSubmissionId", 12345)
      val filenames = Map(
        "Car+Space+16+Access+House%2C+Cray+Avenue+SBizhub+C2817042709470.pdf" -> "Car Space 16 Access House, Cray Avenue SBizhub C2817042709470.pdf",
        "sharpscanner%40gmail.com.pdf" -> "sharpscanner@gmail.com.pdf",
        "Scan+15+Jun+2017%2c+13.04.pdf" -> "Scan 15 Jun 2017, 13.04.pdf",
        "Scan+15+Jun+2017%252c+13.04.pdf" -> "Scan 15 Jun 2017%2c 13.04.pdf"
      )

      for ((encoded, decoded) <- filenames) {
        stubFor(put(urlEqualTo("/customer-management-api/customer/evidence"))
          .withHeader("Ocp-Apim-Subscription-Key", matching(fakeApplication.configuration.getString("voaApi.subscriptionKeyHeader").getOrElse("")))
          .withHeader("Ocp-Apim-Trace", matching(fakeApplication.configuration.getString("voaApi.traceHeader").getOrElse("")))
          .withRequestBody(containing(s"""filename="$decoded""""))
          .withRequestBody(containing("aSubmissionId"))
          .withRequestBody(containing("12345"))
          .willReturn(aResponse().withStatus(200)))

        noException should be thrownBy await(connector.uploadFile(encoded, StreamConverters.fromInputStream { () => new ByteArrayInputStream(file.getBytes) }, metadata))
      }
    }
  }
}
