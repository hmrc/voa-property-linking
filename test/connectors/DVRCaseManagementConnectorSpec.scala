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

package connectors

import java.time.LocalDateTime

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import http.VoaHttpClient
import models.ModernisedEnrichedRequest
import models.dvr.{DetailedValuationRequest, StreamedDocument}
import models.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import play.api.http.ContentTypes
import play.api.libs.ws.{StreamedResponse, WSClient}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp
import play.api.test.Helpers._


class DVRCaseManagementConnectorSpec extends ContentTypes
  with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val mat = fakeApplication.materializer

  implicit val hc = HeaderCarrier()


  val wsClient = fakeApplication.injector.instanceOf[WSClient]
  val voaClient = fakeApplication.injector.instanceOf[VoaHttpClient]
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val connector = new DVRCaseManagementConnector(wsClient, http, voaClient) {
    override lazy val baseURL: String = mockServerUrl
  }

  "request detailed valuation" should {
    "update the valuation with the detailed valuation request" in {

      val dvrUrl = s"/dvr-case-management-api/dvr_case/create_dvr_case"

      val emptyCache ="{}"

      stubFor(post(urlEqualTo(dvrUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(emptyCache)
        )
      )

      val dvr = DetailedValuationRequest(
        authorisationId = 123456,
        organisationId = 9876543,
        personId = 1111111,
        submissionId = "submission1",
        assessmentRef = 24680,
        billingAuthorityReferenceNumber = "barn1"
      )

      val result: Unit = await(connector.requestDetailedValuation(dvr))
      result shouldBe ()
    }
  }

  "get dvr documents" should {
    "return the documents and transfer them into an optional" in {
      implicit val request = ModernisedEnrichedRequest(FakeRequest(), "EXT-1234567", "GG-123456")
      val valuationId = 1L
      val uarn = 2L
      val propertyLinkId = "PL-123456789"

      val dvrUrl = s"/dvr-case-management-api/dvr_case/$uarn/valuation/$valuationId/files"

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
              |     "documentId": 1,
              |     "documentName": "Check Document",
              |     "createDateTime": "$now"
              |     }
              | },
              | "detailedValuation": {
              |    "documentSummary": {
              |       "documentId": 2,
              |       "documentName": "Detailed Valuation Document",
              |       "createDateTime": "$now"
              |    }
              | }
              |}
            """.stripMargin)
        )
      )

      val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
      result shouldBe Some(DvrDocumentFiles(
        checkForm = Document(DocumentSummary(1L, "Check Document", now)),
        detailedValuation = Document(DocumentSummary(2L, "Detailed Valuation Document", now))
      ))
    }

    "return a None upon a 404 from modernised" in {
      implicit val request = ModernisedEnrichedRequest(FakeRequest(), "EXT-1234567", "GG-123456")
      val valuationId = 1L
      val uarn = 2L
      val propertyLinkId = "PL-123456789"

      val dvrUrl = s"/dvr-case-management-api/dvr_case/$uarn/valuation/$valuationId/files"

      stubFor(get(urlEqualTo(dvrUrl))
        .willReturn(aResponse.withStatus(404))
      )

      val result = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
      result shouldBe None
    }
  }


  "get dvr document" should {
    "stream through the file" in {
      implicit val request = ModernisedEnrichedRequest(FakeRequest(), "EXT-1234567", "GG-123456")
      val valuationId = 1L
      val uarn = 2L
      val propertyLinkId = "PL-123456789"
      val fileRef = 1L

      val dvrUrl = s"/dvr-case-management-api/dvr_case/$uarn/valuation/$valuationId/files/$fileRef"

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
