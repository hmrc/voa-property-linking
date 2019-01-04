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

package connectors.fileUpload

import com.kenshoo.play.metrics.Metrics
import connectors.WireMockSpec
import helpers.SimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.circuitbreaker.{CircuitBreakerConfig, UnhealthyServiceException}
import uk.gov.hmrc.play.microservice.filters.MicroserviceFilterSupport
import uk.gov.hmrc.http.HeaderCarrier
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

class FileUploadCircuitBreakerSpec extends WireMockSpec with SimpleWsHttpTestApplication with MicroserviceFilterSupport with MockitoSugar {

  "The file upload connector" when {
    "the circuit is broken" should {
      "not create envelopes on FUaaS" in {
        intercept[UnhealthyServiceException] {
          await(connectorWithBrokenCircuit.createEnvelope(EnvelopeMetadata("2", 2), "some-url")(HeaderCarrier()))
        }

        verify(1, postRequestedFor(urlEqualTo("/file-upload/envelopes")))
      }

      "not get envelope details from FUaaS" in {
        intercept[UnhealthyServiceException] {
          await(connectorWithBrokenCircuit.getEnvelopeDetails("an-envelope-id")(HeaderCarrier()))
        }

        verify(0, getRequestedFor(urlEqualTo("/file-upload/envelopes/an-envelope-id")))
      }

      "not download files from FUaaS" in {
        intercept[UnhealthyServiceException] {
          await(connectorWithBrokenCircuit.downloadFile("a-url")(HeaderCarrier()))
        }

        verify(0, getRequestedFor(urlEqualTo("a-url")))
      }

      "not delete envelopes from FUaaS" in {
        await(connectorWithBrokenCircuit.deleteEnvelope("an-envelope")(HeaderCarrier()))

        verify(0, deleteRequestedFor(urlEqualTo("/file-upload/envelopes/an-envelope")))
      }
    }
  }

  lazy val connectorWithBrokenCircuit = {
    val c = new FileUploadConnector(fakeApplication.injector.instanceOf[SimpleWSHttp],
      mock[Metrics],
      CircuitBreakerConfig("file-upload", 1, 99999, 99999)
    ) {
      override lazy val url = mockServerUrl
    }

    stubFor(post(urlEqualTo("/file-upload/envelopes"))
      .willReturn(aResponse().withStatus(500).withBody("bad stuff happened"))
    )

    await(c.createEnvelope(EnvelopeMetadata("1", 1), "some-url")(HeaderCarrier()))

    verify(1, postRequestedFor(urlEqualTo("/file-upload/envelopes")))

    c
  }

}
