/*
 * Copyright 2021 HM Revenue & Customs
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

import java.net.URI
import java.time.LocalDateTime

import akka.stream.scaladsl.Source
import akka.util.ByteString
import basespecs.BaseUnitSpec
import models.modernised.ValuationHistoryResponse
import models.modernised.externalvaluationmanagement.documents.{Document, DocumentSummary, DvrDocumentFiles}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.ContentTypes
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSCookie, WSRequest, WSResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future
import scala.xml.Elem

class ExternalValuationManagementApiSpec extends BaseUnitSpec with ContentTypes {

  val voaApiUrl = "http://voa-modernised-api/external-valuation-management-api"
  val valuationHistoryUrl = s"$voaApiUrl/properties/{uarn}/valuations"

  trait Setup {
    val uarn: Long = 123456L
    val plSubmissionId: String = "PL12AB34"
    val valuationHistoryResponse: ValuationHistoryResponse = ValuationHistoryResponse(Seq.empty)

    val wsClient = mock[WSClient]
    val config = mock[ServicesConfig]

    val connector = new ExternalValuationManagementApi(wsClient, mockVoaHttpClient, valuationHistoryUrl, config) {
      override lazy val baseURL: String = "http://localhost:9555"
    }
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
    "return the documents and transfer them into an optional" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"

      val now = LocalDateTime.now()

      when(mockVoaHttpClient.GET[Option[DvrDocumentFiles]](any(), any())(any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              DvrDocumentFiles(
                checkForm = Document(DocumentSummary("1", "Check Document", now)),
                detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
              ))))

      val result = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
      result shouldBe Some(
        DvrDocumentFiles(
          checkForm = Document(DocumentSummary("1", "Check Document", now)),
          detailedValuation = Document(DocumentSummary("2", "Detailed Valuation Document", now))
        ))
    }

    "return a None upon a 404 from modernised" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"

      when(mockVoaHttpClient.GET[Option[DvrDocumentFiles]](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      val result = connector.getDvrDocuments(valuationId, uarn, propertyLinkId).futureValue
      result shouldBe None
    }
  }

  "get dvr document" should {
    "stream through the file" in new Setup {
      val valuationId = 1L
      val propertyLinkId = "PL-123456789"
      val fileRef = "1L"

      val mockWsResponse = {
        val m = new WSResponse {
          override def status: Int = 200

          override def statusText: String = ???

          override def headers: Map[String, Seq[String]] = Map()

          override def underlying[T]: T = ???

          override def cookies: Seq[WSCookie] = ???

          override def cookie(name: String): Option[WSCookie] = ???

          override def body: String = ???

          override def bodyAsBytes: ByteString = ???

          override def bodyAsSource: Source[ByteString, _] = Source.empty[ByteString]

          override def allHeaders: Map[String, Seq[String]] = ???

          override def xml: Elem = ???

          override def json: JsValue = ???

          override def uri: URI = ???
        }
        m
      }

      val mockWsRequest = mock[WSRequest]
      when(wsClient.url(any())).thenReturn(mockWsRequest)
      when(mockWsRequest.withHttpHeaders(any())).thenReturn(mockWsRequest)
      when(mockWsRequest.withMethod(any())).thenReturn(mockWsRequest)
      when(mockWsRequest.stream()).thenReturn(Future.successful(mockWsResponse))

      val result = connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef).futureValue
      result shouldBe a[WSResponse]
    }
  }
}
