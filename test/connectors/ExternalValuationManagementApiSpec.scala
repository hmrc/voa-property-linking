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

package connectors

import java.time.LocalDateTime

import basespecs.WireMockSpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor, urlEqualTo, _}
import connectors.externalvaluation.ExternalValuationManagementApi
import helpers.SimpleWsHttpTestApplication
import http.VoaHttpClient
import models.voa.valuation.dvr.StreamedDocument
import models.voa.valuation.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.WithFakeApplication


class ExternalValuationManagementApiSpec extends WireMockSpec with ContentTypes with SimpleWsHttpTestApplication with WithFakeApplication {

  implicit val mat = fakeApplication.materializer

  implicit val hc = HeaderCarrier()

  val wsClient = fakeApplication.injector.instanceOf[WSClient]
  val voaClient = fakeApplication.injector.instanceOf[VoaHttpClient]
  val config = fakeApplication.injector.instanceOf[ServicesConfig]
  val http = fakeApplication.injector.instanceOf[WSHttp]

  val voaApiUrl = "http://voa-modernised-api/external-valuation-management-api"
  val valuationHistoryUrl = s"$voaApiUrl/properties/{uarn}/valuations"


  val connector = new ExternalValuationManagementApi(wsClient, http, voaClient, valuationHistoryUrl, config) {
    override lazy val baseURL: String = mockServerUrl
  }

    "get dvr documents" should {
      "return the documents and transfer them into an optional" in {
        val valuationId = 1L
        val uarn = 2L
        val propertyLinkId = "PL-123456789"

        val dvrUrl = s"/external-valuation-management-api/properties/$uarn/valuations/$valuationId/files?propertyLinkId=PL-123456789"

        val now = LocalDateTime.now()

        stubFor(get(urlEqualTo(dvrUrl))
          .willReturn(aResponse
            .withStatus(200)
            .withHeader("Content-Type", JSON)
            .withBody(
              s"""
                |{
                | "checkForm": {
                |   "documentSummary": {
                |     "documentId": "1",
                |     "documentName": "Check Document",
                |     "createDatetime": "$now"
                |     }
                | },
                | "detailedValuation": {
                |    "documentSummary": {
                |       "documentId": "2",
                |       "documentName": "Detailed Valuation Document",
                |       "createDatetime": "$now"
                |    }
                | }
                |}
              """.stripMargin)
          )
        )

        val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
        result shouldBe Some(DvrDocumentFiles(
          checkForm = Document(DocumentSummary("1", "Check Document", now)),
          detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
        ))
      }

      "return a None upon a 404 from modernised" in {
        val valuationId = 1L
        val uarn = 2L
        val propertyLinkId = "PL-123456789"

        val dvrUrl = s"/external-valuation-management-api/properties/$uarn/valuations/$valuationId/files"

        stubFor(get(urlEqualTo(dvrUrl))
          .willReturn(aResponse.withStatus(404))
        )

        val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
        result shouldBe None
      }
    }


    "get dvr document" should {
      "stream through the file" in {
        val valuationId = 1L
        val uarn = 2L
        val propertyLinkId = "PL-123456789"
        val fileRef = "1L"

        val dvrUrl = s"/external-valuation-management-api/properties/$uarn/valuations/$valuationId/files/$fileRef?propertyLinkId=$propertyLinkId"

        stubFor(get(urlEqualTo(dvrUrl))
          .willReturn(aResponse
            .withStatus(200)
            .withHeader("Content-Type", JSON)
            .withBody(getClass.getResource("/document.pdf").getFile)
          )
        )

        val result = await(connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef))
        result shouldBe a[StreamedDocument]
      }
    }
}

