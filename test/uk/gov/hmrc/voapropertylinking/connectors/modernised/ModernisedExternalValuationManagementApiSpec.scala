/*
 * Copyright 2023 HM Revenue & Customs
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

import basespecs.BaseUnitSpec
import models.modernised.ValuationHistoryResponse
import models.modernised.externalvaluationmanagement.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.ContentTypes
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDateTime
import scala.concurrent.Future

class ModernisedExternalValuationManagementApiSpec extends BaseUnitSpec with ContentTypes {

  val voaApiUrl = "http://voa-modernised-api/external-valuation-management-api"
  val valuationHistoryUrl = s"$voaApiUrl/properties/{uarn}/valuations"

  trait Setup {
    val uarn: Long = 123456L
    val plSubmissionId: String = "PL12AB34"
    val valuationHistoryResponse: ValuationHistoryResponse = ValuationHistoryResponse(Seq.empty)

    val httpClientV2: HttpClientV2 = mock[HttpClientV2]
    val config: ServicesConfig = mock[ServicesConfig]

    val connector: ModernisedExternalValuationManagementApi =
      new ModernisedExternalValuationManagementApi(
        httpClientV2,
        mockVoaHttpClient,
        valuationHistoryUrl,
        config,
        mockAppConfig
      ) {
        override lazy val url: String = "http://localhost:9555"
      }
  }

  "getting a valuation history" should {
    "return the history from modernised" when {
      "there is one for the specified UARN" in new Setup {
        when(mockVoaHttpClient.getWithGGHeaders[Option[ValuationHistoryResponse]](any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(Some(valuationHistoryResponse)))

        connector.getValuationHistory(uarn, plSubmissionId).futureValue shouldBe Some(valuationHistoryResponse)
      }
    }
  }

  "get dvr documents" should {
    "return the documents and transfer them into an optional" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"

      val now: LocalDateTime = LocalDateTime.now()

      when(mockVoaHttpClient.getWithGGHeaders[Option[DvrDocumentFiles]](any())(any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              DvrDocumentFiles(
                checkForm = Document(DocumentSummary("1", "Check Document", now)),
                detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
              )
            )
          )
        )

      val result: Option[DvrDocumentFiles] = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
      result shouldBe Some(
        DvrDocumentFiles(
          checkForm = Document(DocumentSummary("1", "Check Document", now)),
          detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
        )
      )
    }

    // this doesn't happen
    "return a None upon a 404 from modernised" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"

      when(mockVoaHttpClient.getWithGGHeaders[Option[DvrDocumentFiles]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result: Option[DvrDocumentFiles] = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
      result shouldBe None
    }
  }

  "get dvr document" should {
    "stream through the file" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"
      val fileRef = "1L"

      val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
      val mockRequestBuilderWithHeaders: RequestBuilder = mock[RequestBuilder]
      val mockRequestBuilderWithProxy: RequestBuilder = mock[RequestBuilder]
      when(httpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilderWithHeaders)
      when(mockRequestBuilderWithHeaders.withProxy).thenReturn(mockRequestBuilderWithProxy)
      when(mockRequestBuilderWithProxy.stream[HttpResponse](any(), any()))
        .thenReturn(Future.successful(mock[HttpResponse]))

      val result: HttpResponse = connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef).futureValue
      result shouldBe a[HttpResponse]
    }
  }
}
