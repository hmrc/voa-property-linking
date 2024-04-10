/*
 * Copyright 2024 HM Revenue & Customs
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

import models.PaginationParams
import models.modernised.externalpropertylink.myclients._
import models.modernised.externalpropertylink.myorganisations._
import models.modernised.externalpropertylink.requests.{CreatePropertyLink, CreatePropertyLinkOnClientBehalf}
import models.modernised._
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, JsValidationException}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.binders.clients.GetClientsParameters
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.{GetClientPropertyLinksParameters, GetMyClientsPropertyLinkParameters, GetMyOrganisationPropertyLinksParameters}
import uk.gov.hmrc.voapropertylinking.stubs.modernised.ModernisedExternalPropertyLinkingStub

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext

class ModernisedExternalPropertyLinkingManagementClientsISpec
    extends BaseIntegrationSpec with ModernisedExternalPropertyLinkingStub {

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    lazy val connector: ModernisedExternalPropertyLinkApi = app.injector.instanceOf[ModernisedExternalPropertyLinkApi]
  }

  "getMyOrganisationsPropertyLinks" should {

    val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)
    val getMyOrganisationPropertyLinksParameters =
      GetMyOrganisationPropertyLinksParameters(
        address = Some("1 HIGH STREET, BRIGHTON"),
        uarn = Some(33333L),
        baref = Some("test_baref"),
        agent = Some("test_agent"),
        client = Some("test_client"),
        status = Some("APPROVED"),
        sortField = Some("test_field"),
        sortOrder = Some("test_sort")
      )

    "return a PropertyLinksWithAgents model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "start" : 1,
           |  "size" : 1,
           |  "filterTotal" : 1,
           |  "total" : 1,
           |  "authorisations" : [ {
           |    "authorisationId" : 11111,
           |    "status" : "APPROVED",
           |    "startDate" : "2018-09-05",
           |    "endDate" : "2018-09-05",
           |    "submissionId" : "22222",
           |    "uarn" : 33333,
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "localAuthorityRef" : "44444",
           |    "agents" : [ {
           |      "authorisedPartyId" : 24680,
           |      "organisationId" : 123456,
           |      "organisationName" : "org name",
           |      "representativeCode" : 1111
           |    } ]
           |  } ]
           |}
           |""".stripMargin
      )

      val date: LocalDate = LocalDate.parse("2018-09-05")
      val summaryPropertyLinkWithAgents: SummaryPropertyLinkWithAgents =
        SummaryPropertyLinkWithAgents(
          authorisationId = 11111,
          status = PropertyLinkStatus.APPROVED,
          startDate = date,
          endDate = Some(date),
          submissionId = "22222",
          uarn = 33333,
          address = "1 HIGH STREET, BRIGHTON",
          localAuthorityRef = "44444",
          agents = Seq(
            AgentDetails(
              authorisedPartyId = 24680,
              organisationId = 123456,
              organisationName = "org name",
              representativeCode = 1111
            ))
        )
      val responseModel: PropertyLinksWithAgents =
        PropertyLinksWithAgents(
          start = 1,
          size = 1,
          filterTotal = 1,
          total = 1,
          authorisations = Seq(summaryPropertyLinkWithAgents))

      stubGetMyOrganisationsPropertyLinks(
        searchParams = getMyOrganisationPropertyLinksParameters,
        params = paginationParams)(OK, responseJson)

      val result: PropertyLinksWithAgents = await {
        connector.getMyOrganisationsPropertyLinks(getMyOrganisationPropertyLinksParameters, Some(paginationParams))
      }

      result shouldBe responseModel
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyOrganisationsPropertyLinks(
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getMyOrganisationsPropertyLinks(getMyOrganisationPropertyLinksParameters, Some(paginationParams))
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyOrganisationsPropertyLinks(
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getMyOrganisationsPropertyLinks(getMyOrganisationPropertyLinksParameters, Some(paginationParams))
          )
        }
      }
    }
  }

  "getMyOrganisationsPropertyLink" should {
    val submissionId = "22222"
    "return a PropertyLinksWithAgents model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "authorisation" : {
           |    "authorisationId" : 11111,
           |    "status" : "APPROVED",
           |    "startDate" : "2018-09-05",
           |    "endDate" : "2018-09-05",
           |    "submissionId" : "$submissionId",
           |    "capacity" : "OWNER",
           |    "uarn" : 33333,
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "localAuthorityRef" : "44444",
           |    "agents" : [ {
           |      "authorisedPartyId" : 24680,
           |      "organisationId" : 123456,
           |      "organisationName" : "org name",
           |      "representativeCode" : 1111
           |    } ]
           |  }
           |}
           |""".stripMargin
      )
      val date: LocalDate = LocalDate.parse("2018-09-05")
      val propertyLinkWithAgents: PropertyLinkWithAgents =
        PropertyLinkWithAgents(
          authorisationId = 11111,
          status = PropertyLinkStatus.APPROVED,
          startDate = date,
          endDate = Some(date),
          submissionId = "22222",
          capacity = "OWNER",
          uarn = 33333,
          address = "1 HIGH STREET, BRIGHTON",
          localAuthorityRef = "44444",
          agents = Seq(
            AgentDetails(
              authorisedPartyId = 24680,
              organisationId = 123456,
              organisationName = "org name",
              representativeCode = 1111
            )
          )
        )
      val responseModel: OwnerPropertyLink = OwnerPropertyLink(authorisation = propertyLinkWithAgents)

      stubGetMyOrganisationsPropertyLink(submissionId)(OK, responseJson)

      val result: Option[OwnerPropertyLink] = await {
        connector.getMyOrganisationsPropertyLink(submissionId)
      }

      result shouldBe Some(responseModel)
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyOrganisationsPropertyLink(submissionId)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getMyOrganisationsPropertyLink(submissionId)
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyOrganisationsPropertyLink(submissionId)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getMyOrganisationsPropertyLink(submissionId)
          )
        }
      }
    }
  }

  "getClientsPropertyLinks" should {
    val date: LocalDate = LocalDate.parse("2018-09-05")
    val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)
    val getMyClientsPropertyLinksParameters =
      GetMyClientsPropertyLinkParameters(
        address = Some("1 HIGH STREET, BRIGHTON"),
        baref = Some("test_baref"),
        client = Some("test_client"),
        status = Some("APPROVED"),
        sortField = Some("test_field"),
        sortOrder = Some("test_sort"),
        representationStatus = Some("test_rep_status"),
        appointedFromDate = Some(date),
        appointedToDate = Some(date)
      )
    "return a PropertyLinksWithClient model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "start" : 1,
           |  "size" : 1,
           |  "filterTotal" : 1,
           |  "total" : 1,
           |  "authorisations" : [ {
           |    "authorisationId" : 11111,
           |    "authorisedPartyId" : 11111,
           |    "status" : "APPROVED",
           |    "startDate" : "2018-09-05",
           |    "endDate" : "2018-09-05",
           |    "submissionId" : "22222",
           |    "uarn" : 33333,
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "localAuthorityRef" : "44444",
           |    "appointedDate" : "2018-09-05",
           |    "client" : {
           |      "organisationId" : 55555,
           |      "organisationName" : "mock org"
           |    }
           |  } ]
           |}
           |""".stripMargin
      )
      val propertyLinksWithClient: PropertyLinksWithClient =
        PropertyLinksWithClient(
          start = 1,
          size = 1,
          filterTotal = 1,
          total = 1,
          authorisations = Seq(
            SummaryPropertyLinkWithClient(
              authorisationId = 11111,
              authorisedPartyId = 11111,
              status = PropertyLinkStatus.APPROVED,
              startDate = date,
              endDate = Some(date),
              submissionId = "22222",
              uarn = 33333,
              address = "1 HIGH STREET, BRIGHTON",
              localAuthorityRef = "44444",
              appointedDate = date,
              client = ClientDetails(55555, "mock org")
            )
          )
        )

      stubGetClientsPropertyLinks(getMyClientsPropertyLinksParameters, paginationParams)(OK, responseJson)

      val result: Option[PropertyLinksWithClient] = await {
        connector.getClientsPropertyLinks(getMyClientsPropertyLinksParameters, Some(paginationParams))
      }

      result shouldBe Some(propertyLinksWithClient)
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetClientsPropertyLinks(getMyClientsPropertyLinksParameters, paginationParams)(
          OK,
          Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getClientsPropertyLinks(getMyClientsPropertyLinksParameters, Some(paginationParams))
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetClientsPropertyLinks(getMyClientsPropertyLinksParameters, paginationParams)(
          INTERNAL_SERVER_ERROR,
          Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getClientsPropertyLinks(getMyClientsPropertyLinksParameters, Some(paginationParams))
          )
        }
      }
    }
  }

  "getClientPropertyLinks" should {
    val clientOrgID: Long = 123456L
    val date: LocalDate = LocalDate.parse("2018-09-05")
    val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)
    val getClientPropertyLinksParameters: GetClientPropertyLinksParameters =
      GetClientPropertyLinksParameters(
        address = Some("1 HIGH STREET, BRIGHTON"),
        baref = Some("test_baref"),
        status = Some("APPROVED"),
        sortField = Some("test_field"),
        sortOrder = Some("test_sort"),
        representationStatus = Some("test_rep_status"),
        appointedFromDate = Some(date),
        appointedToDate = Some(date),
        uarn = Some(7654321L),
        client = Some("test_client")
      )
    "return a PropertyLinksWithClient model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "start" : 1,
           |  "size" : 1,
           |  "filterTotal" : 1,
           |  "total" : 1,
           |  "authorisations" : [ {
           |    "authorisationId" : 11111,
           |    "authorisedPartyId" : 11111,
           |    "status" : "APPROVED",
           |    "startDate" : "2018-09-05",
           |    "endDate" : "2018-09-05",
           |    "submissionId" : "22222",
           |    "uarn" : 7654321,
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "localAuthorityRef" : "44444",
           |    "appointedDate" : "2018-09-05",
           |    "client" : {
           |      "organisationId" : 55555,
           |      "organisationName" : "mock org"
           |    }
           |  } ]
           |}
           |""".stripMargin
      )
      val propertyLinksWithClient: PropertyLinksWithClient =
        PropertyLinksWithClient(
          start = 1,
          size = 1,
          filterTotal = 1,
          total = 1,
          authorisations = Seq(
            SummaryPropertyLinkWithClient(
              authorisationId = 11111,
              authorisedPartyId = 11111,
              status = PropertyLinkStatus.APPROVED,
              startDate = date,
              endDate = Some(date),
              submissionId = "22222",
              uarn = 7654321L,
              address = "1 HIGH STREET, BRIGHTON",
              localAuthorityRef = "44444",
              appointedDate = date,
              client = ClientDetails(55555, "mock org")
            )
          )
        )

      stubGetClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, paginationParams)(OK, responseJson)

      val result: Option[PropertyLinksWithClient] = await {
        connector.getClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, Some(paginationParams))
      }

      result shouldBe Some(propertyLinksWithClient)
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, paginationParams)(
          OK,
          Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, Some(paginationParams))
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, paginationParams)(
          INTERNAL_SERVER_ERROR,
          Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getClientPropertyLinks(clientOrgID, getClientPropertyLinksParameters, Some(paginationParams))
          )
        }
      }
    }
  }

  "getClientsPropertyLink" should {
    val submissionId = "22222"
    "return a ClientPropertyLink model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse(
        s"""
           |{
           |  "authorisation" : {
           |    "authorisationId" : 11111,
           |    "authorisedPartyId" : 11111,
           |    "status" : "APPROVED",
           |    "startDate" : "2018-09-05",
           |    "endDate" : "2018-09-05",
           |    "submissionId" : "22222",
           |    "capacity" : "OWNER",
           |    "uarn" : 7654321,
           |    "address" : "1 HIGH STREET, BRIGHTON",
           |    "localAuthorityRef" : "44444",
           |    "client" : {
           |      "organisationId" : 55555,
           |      "organisationName" : "mock org"
           |    }
           |  }
           |}
           |""".stripMargin
      )
      val date: LocalDate = LocalDate.parse("2018-09-05")
      val clientPropertyLink: ClientPropertyLink =
        ClientPropertyLink(
          authorisation = PropertyLinkWithClient(
            authorisationId = 11111,
            authorisedPartyId = 11111,
            status = PropertyLinkStatus.APPROVED,
            startDate = date,
            endDate = Some(date),
            submissionId = submissionId,
            capacity = "OWNER",
            uarn = 7654321L,
            address = "1 HIGH STREET, BRIGHTON",
            localAuthorityRef = "44444",
            client = ClientDetails(55555, "mock org")
          )
        )

      stubGetClientsPropertyLink(submissionId)(OK, responseJson)

      val result: Option[ClientPropertyLink] = await {
        connector.getClientsPropertyLink(submissionId)
      }

      result shouldBe Some(clientPropertyLink)
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetClientsPropertyLink(submissionId)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getClientsPropertyLink(submissionId)
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetClientsPropertyLink(submissionId)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getClientsPropertyLink(submissionId)
          )
        }
      }
    }
  }

  "getMyClients" should {
    val date: LocalDate = LocalDate.parse("2018-09-05")
    val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)
    val getClientsParams = GetClientsParameters(
      name = Some("test_name"),
      appointedFromDate = Some(date),
      appointedToDate = Some(date)
    )
    "return a ClientsResponse model on success" in new TestSetup {
      val responseJson: JsValue = Json.parse("""
                                               |{
                                               |  "resultCount" : 1,
                                               |  "clients" : [ {
                                               |    "organisationId" : 123456,
                                               |    "name" : "Org Name",
                                               |    "appointedDate" : "2018-09-05",
                                               |    "propertyCount" : 1
                                               |  } ]
                                               |}
                                               |""".stripMargin)
      val clientsResponse: ClientsResponse = ClientsResponse(
        resultCount = Some(1),
        clients = List(
          Client(
            organisationId = 123456L,
            name = "Org Name",
            appointedDate = date,
            propertyCount = 1
          )
        )
      )

      stubGetMyClients(getClientsParams, paginationParams)(OK, responseJson)

      val result: ClientsResponse = await {
        connector.getMyClients(getClientsParams, Some(paginationParams))
      }

      result shouldBe clientsResponse
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyClients(getClientsParams, paginationParams)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getMyClients(getClientsParams, Some(paginationParams))
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyClients(getClientsParams, paginationParams)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getMyClients(getClientsParams, Some(paginationParams))
          )
        }
      }
    }
  }

  "createPropertyLink" should {
    val date: String = "2018-09-05"
    val dateTimeString: String = "2007-12-03T10:15:30"
    val localDate: LocalDate = LocalDate.parse(date)
    val requestJson = Json.parse(
      s"""
         |{
         |  "uarn" : 11111,
         |  "capacity" : "OWNER",
         |  "startDate" : "2018-09-05",
         |  "endDate" : "2018-09-05",
         |  "method" : "RATES_BILL",
         |  "PLsubmissionId" : "44444",
         |  "createDatetime" : "2007-12-03T10:15:30.000Z",
         |  "uploadedFiles" : [ {
         |    "name" : "FILE_NAME",
         |    "evidenceType" : "ratesBill"
         |  } ],
         |  "submissionSource" : "DFE_UI"
         |}
         |""".stripMargin
    )
    val createPropertyLink: CreatePropertyLink = CreatePropertyLink(
      uarn = 11111,
      capacity = Capacity.withName("OWNER"),
      startDate = localDate,
      endDate = Some(localDate),
      method = ProvidedEvidence.withName("RATES_BILL"),
      PLsubmissionId = "44444",
      createDatetime = LocalDateTime.parse(dateTimeString),
      uploadedFiles = Seq(Evidence("FILE_NAME", EvidenceType.RATES_BILL)),
      submissionSource = "DFE_UI"
    )

    "return a HttpResponse on success" in new TestSetup {
      val response: HttpResponse = HttpResponse(OK, Json.obj("any" -> "body"), Map.empty)

      stubCreatePropertyLink(requestJson)(response)

      val result: HttpResponse = await {
        connector.createPropertyLink(createPropertyLink)
      }
      result.status shouldBe response.status
      result.json shouldBe response.json
    }
    "throw an exception" when {
      "any error status code is returned" in new TestSetup {
        val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.obj("any" -> "body"), Map.empty)

        stubCreatePropertyLink(requestJson)(response)

        assertThrows[Exception] {
          await(
            connector.createPropertyLink(createPropertyLink)
          )
        }
      }
    }
  }

  "createOnClientBehalf" should {
    val clientId: Long = 11111L
    val date: String = "2018-09-05"
    val dateTimeString: String = "2007-12-03T10:15:30"
    val localDate: LocalDate = LocalDate.parse(date)
    val requestJson = Json.parse(
      s"""
         |{
         |  "uarn" : $clientId,
         |  "capacity" : "OWNER",
         |  "startDate" : "2018-09-05",
         |  "endDate" : "2018-09-05",
         |  "method" : "RATES_BILL",
         |  "propertyLinkSubmissionId" : "44444",
         |  "createDatetime" : "2007-12-03T10:15:30.000Z",
         |  "evidence" : [ {
         |    "name" : "FILE_NAME",
         |    "evidenceType" : "ratesBill"
         |  } ],
         |  "submissionSource" : "DFE_UI"
         |}
         |""".stripMargin
    )
    val createPropertyLink: CreatePropertyLinkOnClientBehalf = CreatePropertyLinkOnClientBehalf(
      uarn = clientId,
      capacity = Capacity.withName("OWNER"),
      startDate = localDate,
      endDate = Some(localDate),
      method = ProvidedEvidence.withName("RATES_BILL"),
      propertyLinkSubmissionId = "44444",
      createDatetime = LocalDateTime.parse(dateTimeString),
      evidence = Seq(Evidence("FILE_NAME", EvidenceType.RATES_BILL)),
      submissionSource = "DFE_UI"
    )
    "return a HttpResponse on success" in new TestSetup {
      val response: HttpResponse = HttpResponse(OK, Json.obj("any" -> "body"), Map.empty)

      stubCreateOnClientBehalf(requestJson, clientId)(response)

      val result: HttpResponse = await {
        connector.createOnClientBehalf(createPropertyLink, clientId)
      }
      result.status shouldBe response.status
      result.json shouldBe response.json
    }
    "throw an exception" when {
      "any error status code is returned" in new TestSetup {
        val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.obj("any" -> "body"), Map.empty)

        stubCreateOnClientBehalf(requestJson, clientId)(response)

        assertThrows[Exception] {
          await(
            connector.createOnClientBehalf(createPropertyLink, clientId)
          )
        }
      }
    }
  }

  "revokeClientProperty" should {
    val plSubmissionId = "44444"
    "return a success if delete was successful" in new TestSetup {
      val response: HttpResponse = HttpResponse(OK, Json.obj("any" -> "body"), Map.empty)

      stubRevokeClientProperty(plSubmissionId)(response)

      val result = await(connector.revokeClientProperty(plSubmissionId))

      result shouldBe ()
    }
    "return an exception when any error is returned" in new TestSetup {
      val response: HttpResponse = HttpResponse(INTERNAL_SERVER_ERROR, Json.obj("any" -> "body"), Map.empty)

      stubRevokeClientProperty(plSubmissionId)(response)

      assertThrows[Exception] {
        await(connector.revokeClientProperty(plSubmissionId))
      }
    }
  }
}
