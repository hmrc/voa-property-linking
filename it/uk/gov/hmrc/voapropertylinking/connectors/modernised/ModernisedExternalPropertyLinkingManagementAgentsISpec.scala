package uk.gov.hmrc.voapropertylinking.connectors.modernised

import models.PaginationParams
import models.modernised.PropertyLinkStatus
import models.modernised.externalpropertylink.myorganisations.{AgentDetails, AgentList, AgentSummary, PropertyLinksWithAgents, SummaryPropertyLinkWithAgents}
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.auth.{Principal, RequestWithPrincipal}
import uk.gov.hmrc.voapropertylinking.binders.propertylinks.GetMyOrganisationPropertyLinksParameters
import uk.gov.hmrc.voapropertylinking.stubs.modernised.ModernisedExternalPropertyLinkingStub
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes.INTERNAL_SERVER_ERROR

import java.time.LocalDate
import scala.concurrent.ExecutionContext

class ModernisedExternalPropertyLinkingManagementAgentsISpec
    extends BaseIntegrationSpec with ModernisedExternalPropertyLinkingStub {

  trait TestSetup {
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
    implicit val request: RequestWithPrincipal[AnyContentAsEmpty.type] =
      RequestWithPrincipal(FakeRequest(), Principal(externalId = "testExternalId", groupId = "testGroupId"))

    lazy val connector: ModernisedExternalPropertyLinkApi = app.injector.instanceOf[ModernisedExternalPropertyLinkApi]
  }

  "getMyAgentPropertyLinks" should {

    val agentCode = 12345678L
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
      val responseJson = Json.parse(
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

      val date = LocalDate.parse("2018-09-05")
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

      stubGetMyAgentPropertyLinks(
        agentCode = agentCode,
        searchParams = getMyOrganisationPropertyLinksParameters,
        params = paginationParams)(OK, responseJson)

      val result: PropertyLinksWithAgents = await {
        connector.getMyAgentPropertyLinks(agentCode, getMyOrganisationPropertyLinksParameters, paginationParams)
      }

      result shouldBe responseModel
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyAgentPropertyLinks(
          agentCode = agentCode,
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getMyAgentPropertyLinks(agentCode, getMyOrganisationPropertyLinksParameters, paginationParams)
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyAgentPropertyLinks(
          agentCode = agentCode,
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getMyAgentPropertyLinks(agentCode, getMyOrganisationPropertyLinksParameters, paginationParams)
          )
        }
      }
    }
  }

  "getMyAgentAvailablePropertyLinks" should {
    val agentCode = 12345678L
    val paginationParams = PaginationParams(startPoint = 1, pageSize = 1, requestTotalRowCount = true)
    val getMyOrganisationPropertyLinksParameters =
      GetMyOrganisationPropertyLinksParameters(
        address = Some("1 HIGH STREET, BRIGHTON"),
        agent = Some("test_agent"),
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

      val date = LocalDate.parse("2018-09-05")
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

      stubGetMyAgentAvailablePropertyLinks(
        agentCode = agentCode,
        searchParams = getMyOrganisationPropertyLinksParameters,
        params = paginationParams)(OK, responseJson)

      val result: PropertyLinksWithAgents = await {
        connector
          .getMyAgentAvailablePropertyLinks(agentCode, getMyOrganisationPropertyLinksParameters, Some(paginationParams))
      }

      result shouldBe responseModel
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyAgentAvailablePropertyLinks(
          agentCode = agentCode,
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(
            connector.getMyAgentAvailablePropertyLinks(
              agentCode,
              getMyOrganisationPropertyLinksParameters,
              Some(paginationParams))
          )
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyAgentAvailablePropertyLinks(
          agentCode = agentCode,
          searchParams = getMyOrganisationPropertyLinksParameters,
          params = paginationParams)(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(
            connector.getMyAgentAvailablePropertyLinks(
              agentCode,
              getMyOrganisationPropertyLinksParameters,
              Some(paginationParams))
          )
        }
      }
    }
  }

  "getMyOrganisationsAgents" should {
    "return an AgentsList on success" in new TestSetup {
      val date = LocalDate.parse("2023-04-02")
      val responseJson: JsValue = Json.parse(
        s"""
          |{
          |"resultCount" : 1,
          |  "agents" : [ {
          |    "organisationId" : 1,
          |    "representativeCode" : 987,
          |    "name" : "Some Agent Org",
          |    "appointedDate" : "$date",
          |    "propertyCount" : 2,
          |    "listYears" : ["2017"]
          |  } ]
          |}
          |""".stripMargin
      )
      val agentsList: AgentList = AgentList(
        resultCount = 1,
        agents = List(
          AgentSummary(
            organisationId = 1L,
            representativeCode = 987L,
            name = "Some Agent Org",
            appointedDate = date,
            propertyCount = 2,
            listYears = Some(List("2017"))
          )
        )
      )

      stubGetMyOrganisationsAgents()(OK, responseJson)

      val result: AgentList = await(connector.getMyOrganisationsAgents())

      result shouldBe agentsList
    }

    "return an AgentsList without listYears if not present on success" in new TestSetup {
      val date = LocalDate.parse("2023-04-02")
      val responseJson: JsValue = Json.parse(
        s"""
          |{
          |"resultCount" : 1,
          |  "agents" : [ {
          |    "organisationId" : 1,
          |    "representativeCode" : 987,
          |    "name" : "Some Agent Org",
          |    "appointedDate" : "$date",
          |    "propertyCount" : 2
          |  } ]
          |}
          |""".stripMargin
      )
      val agentsList: AgentList = AgentList(
        resultCount = 1,
        agents = List(
          AgentSummary(
            organisationId = 1L,
            representativeCode = 987L,
            name = "Some Agent Org",
            appointedDate = date,
            propertyCount = 2
          )
        )
      )

      stubGetMyOrganisationsAgents()(OK, responseJson)

      val result: AgentList = await(connector.getMyOrganisationsAgents())

      result shouldBe agentsList
    }

    "throw an exception" when {
      "a success code is received but with an invalid body" in new TestSetup {
        stubGetMyOrganisationsAgents()(OK, Json.obj("invalid" -> "body"))

        assertThrows[JsValidationException] {
          await(connector.getMyOrganisationsAgents())
        }
      }

      "any error status code is returned" in new TestSetup {
        stubGetMyOrganisationsAgents()(INTERNAL_SERVER_ERROR, Json.obj("doesnt" -> "matter"))

        assertThrows[Exception] {
          await(connector.getMyOrganisationsAgents())
        }
      }
    }
  }
}
