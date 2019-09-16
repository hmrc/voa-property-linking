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

package uk.gov.hmrc.voapropertylinking.connectors.modernised

import java.time.LocalDateTime

import basespecs.WireMockSpec
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, stubFor, urlEqualTo, _}
import helpers.SimpleWsHttpTestApplication
import models.voa.valuation.dvr.StreamedDocument
import models.voa.valuation.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.WithFakeApplication
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import scala.concurrent.Future

class ExternalValuationManagementApiSpec extends WireMockSpec with ContentTypes with SimpleWsHttpTestApplication with WithFakeApplication {

  implicit val mat = fakeApplication.materializer

  val wsClient = fakeApplication.injector.instanceOf[WSClient]
  val config = fakeApplication.injector.instanceOf[ServicesConfig]
  val http = mock[VoaHttpClient]

  val voaApiUrl = "http://voa-modernised-api/external-valuation-management-api"
  val valuationHistoryUrl = s"$voaApiUrl/properties/{uarn}/valuations"


  val connector = new ExternalValuationManagementApi(wsClient, http, valuationHistoryUrl, config) {
    override lazy val baseURL: String = mockServerUrl
  }

    "get dvr documents" should {
      "return the documents and transfer them into an optional" in {
        val valuationId = 1L
        val uarn = 2L
        val propertyLinkId = "PL-123456789"

        val now = LocalDateTime.now()

        when(http.GET[DvrDocumentFiles](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(DvrDocumentFiles(
            checkForm = Document(DocumentSummary("1", "Check Document", now)),
            detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
          )))

        val result = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
        result shouldBe Some(DvrDocumentFiles(
          checkForm = Document(DocumentSummary("1", "Check Document", now)),
          detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
        ))
      }

      "return a None upon a 404 from modernised" in {
        val valuationId = 1L
        val uarn = 2L
        val propertyLinkId = "PL-123456789"

        when(http.GET[DvrDocumentFiles](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.failed(new NotFoundException("not found dvr documents")))

        val result = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
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

        val result = connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef).futureValue
        result shouldBe a[StreamedDocument]
      }
    }
}

