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

import java.nio.file.Files.readAllBytes
import java.nio.file.Paths
import java.time.LocalDateTime

import basespecs.WireMockSpec
import com.github.tomakehurst.wiremock.client.WireMock._
import models.modernised.ValuationHistoryResponse
import models.voa.valuation.dvr.StreamedDocument
import models.voa.valuation.dvr.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito.when
import play.api.http.ContentTypes
import play.api.libs.ws.WSClient
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.test.WithFakeApplication

import scala.concurrent.Future

class ExternalValuationManagementApiSpec extends WireMockSpec with ContentTypes with WithFakeApplication {

  implicit val mat = fakeApplication.materializer

  val wsClient = fakeApplication.injector.instanceOf[WSClient]
  val config = fakeApplication.injector.instanceOf[ServicesConfig]

  val voaApiUrl = "http://voa-modernised-api/external-valuation-management-api"
  val valuationHistoryUrl = s"$voaApiUrl/properties/{uarn}/valuations"

  trait Setup {
    val uarn: Long = 123456L
    val plSubmissionId: String = "PL12AB34"
    val valuationHistoryResponse: ValuationHistoryResponse = ValuationHistoryResponse(Seq.empty)
  }

  val connector = new ExternalValuationManagementApi(wsClient, mockVoaHttpClient, valuationHistoryUrl, config) {
    override lazy val baseURL: String = mockServerUrl
  }

  "getting a valuation history" should {
    "return the history from modernised" when {
      "there is one for the specified UARN" in new Setup {
        when(mockVoaHttpClient.GET[Option[ValuationHistoryResponse]](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(Some(valuationHistoryResponse)))

        connector.getValuationHistory(uarn, plSubmissionId).futureValue shouldBe Some(valuationHistoryResponse)
      }
    }
  }

  "get dvr documents" should {
    "return the documents and transfer them into an optional" in {
      val valuationId = 1L
      val uarn = 2L
      val propertyLinkId = "PL-123456789"

      val now = LocalDateTime.now()

      when(mockVoaHttpClient.GET[DvrDocumentFiles](any(), any())(any(), any(), any(), any()))
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

      when(mockVoaHttpClient.GET[DvrDocumentFiles](any(), any())(any(), any(), any(), any()))
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

      stubFor(get(urlEqualTo(dvrUrl)).willReturn(ok(new String(readAllBytes(Paths.get(getClass.getResource("/document.pdf").toURI))))))

      val result = connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef).futureValue
      result shouldBe a[StreamedDocument]
    }
  }
}

