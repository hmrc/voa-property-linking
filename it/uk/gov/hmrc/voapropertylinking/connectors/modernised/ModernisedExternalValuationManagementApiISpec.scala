package uk.gov.hmrc.voapropertylinking.connectors.modernised

import models.modernised.externalvaluationmanagement.documents.{Document, DocumentSummary, DvrDocumentFiles}
import models.modernised.{AllowedAction, ListType, ValuationHistory, ValuationHistoryResponse}
import org.mockito.Mockito
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{BAD_REQUEST, NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.stubs.modernised.ModernisedExternalValuationManagementStub
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes.INTERNAL_SERVER_ERROR

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext

class ModernisedExternalValuationManagementApiISpec
  extends BaseIntegrationSpec with ModernisedExternalValuationManagementStub {

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    lazy val connector: ModernisedExternalValuationManagementApi =
      app.injector.instanceOf[ModernisedExternalValuationManagementApi]
  }

  "getDvrDocuments" should {

    val valuationId: Long = 123456789L
    val uarn: Long = 987654321L
    val propertyLinkId: String = "test-property-link-id"
    val time: LocalDateTime = LocalDateTime.parse("2019-09-11T11:03:25.123")

    "return a DvrDocumentFiles on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           | "checkForm": {
           |   "documentSummary": {
           |     "documentId": "1L",
           |     "documentName": "Check Document",
           |     "createDatetime": "$time"
           |     }
           | },
           | "detailedValuation": {
           |    "documentSummary": {
           |       "documentId": "2L",
           |       "documentName": "Detailed Valuation Document",
           |       "createDatetime": "$time"
           |    }
           | }
           |}
           |""".stripMargin
      )
      val dvrDocumentFiles: DvrDocumentFiles = DvrDocumentFiles(
        checkForm = Document(DocumentSummary("1L", "Check Document", time)),
        detailedValuation = Document(DocumentSummary("2L", "Detailed Valuation Document", time))
      )

      stubGetDvrDocuments(valuationId, uarn, propertyLinkId)(OK, responseJson)

      val result: Option[DvrDocumentFiles] = await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))

      result shouldBe Some(dvrDocumentFiles)
    }

    "throw an exception" when {
      "a success status is returned but with an invalid body" in new TestSetup {
        stubGetDvrDocuments(valuationId, uarn, propertyLinkId)(OK, Json.obj())

        assertThrows[Exception] {
          await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
        }
      }
      "any error status is received" in new TestSetup {
        stubGetDvrDocuments(valuationId, uarn, propertyLinkId)(NOT_FOUND, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getDvrDocuments(valuationId, uarn, propertyLinkId))
        }
      }
    }
  }

  "getValuationHistory" should {
    val date = LocalDate.parse("2018-09-05")
    val uarn: Long = 987654321L
    val propertyLinkId: String = "test-property-link-id"

    "return a DvrDocumentFiles on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "NDRListValuationHistoryItems" : [ {
           |    "asstRef" : 125689,
           |    "listYear" : "2017",
           |    "uarn" : 923411,
           |    "billingAuthorityReference" : "VOA1",
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "effectiveDate" : "$date",
           |    "rateableValue" : 2599,
           |    "listType" : "current",
           |    "allowedActions" : [ "viewDetailedValuation" ]
           |  } ]
           |}
           |""".stripMargin
      )
      val valuationHistoryResponse: ValuationHistoryResponse =
        ValuationHistoryResponse(
          Seq(
            ValuationHistory(
              asstRef = 125689,
              listYear = "2017",
              uarn = 923411,
              billingAuthorityReference = "VOA1",
              address = "1 HIGH STREET, BRIGHTON",
              description = None,
              specialCategoryCode = None,
              compositeProperty = None,
              effectiveDate = Some(date),
              listAlterationDate = None,
              numberOfPreviousProposals = None,
              settlementCode = None,
              totalAreaM2 = None,
              costPerM2 = None,
              rateableValue = Some(2599),
              transitionalCertificate = None,
              deletedIndicator = None,
              valuationDetailsAvailable = None,
              billingAuthCode = None,
              listType = ListType.CURRENT,
              allowedActions = List(AllowedAction.VIEW_DETAILED_VALUATION)
            )
          )
        )

      stubGetValuationHistory(uarn, propertyLinkId)(OK, responseJson)

      val result: Option[ValuationHistoryResponse] = await(connector.getValuationHistory(uarn, propertyLinkId))

      result shouldBe Some(valuationHistoryResponse)
    }

    "throw an exception" when {
      "a success status is returned but with an invalid body" in new TestSetup {
        stubGetValuationHistory(uarn, propertyLinkId)(OK, Json.obj())

        assertThrows[Exception] {
          await(connector.getValuationHistory(uarn, propertyLinkId))        }
      }
      "any error status is received" in new TestSetup {
        stubGetValuationHistory(uarn, propertyLinkId)(NOT_FOUND, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getValuationHistory(uarn, propertyLinkId))        }
      }
    }
  }

  "getDvrDocument" should {

    val valuationId: Long = 123456789L
    val uarn: Long = 987654321L
    val propertyLinkId: String = "test-property-link-id"
    val fileRef: String = "test-fileref"

    "return a WsResponse on success" in new TestSetup {
      val response: WSResponse = mock[WSResponse]
      Mockito.when(response.status).thenReturn(OK)

      stubGetDvrDocument(valuationId, uarn, propertyLinkId, fileRef)(response)

      val result: WSResponse = await(connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef))

      result.status shouldBe OK
      result shouldBe a [WSResponse]
    }

    "return an upstream error response" when {
      "a 4xx status is returned" in new TestSetup {
        val response: WSResponse = mock[WSResponse]
        Mockito.when(response.status).thenReturn(BAD_REQUEST)

        stubGetDvrDocument(valuationId, uarn, propertyLinkId, fileRef)(response)

        assertThrows[UpstreamErrorResponse] {
          await(connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef))        }
      }
      "a 5xx status is returned" in new TestSetup {
        val response: WSResponse = mock[WSResponse]
        Mockito.when(response.status).thenReturn(INTERNAL_SERVER_ERROR)

        stubGetDvrDocument(valuationId, uarn, propertyLinkId, fileRef)(response)

        assertThrows[UpstreamErrorResponse] {
          await(connector.getDvrDocument(valuationId, uarn, propertyLinkId, fileRef))        }
      }
    }
  }

}
