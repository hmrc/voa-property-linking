/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time._

import basespecs.BaseUnitSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.voapropertylinking.models.modernised.agentrepresentation.{AgentOrganisation, OrganisationLatestDetail}

class CustomerManagementApiSpec extends BaseUnitSpec {

  val defaultHttpClient = mock[DefaultHttpClient]
  val config = mockServicesConfig
  val testConnector = new CustomerManagementApi(defaultHttpClient, config, "agentByRepresentationCodeUrl") {
    override lazy val baseUrl = "http://some-uri"
  }

  val url = s"/customer-management-api/person"

  "CustomerManagementApi.getDetailedIndividual" should {
    "return the individual account associated with the provided person id" in {
      val id = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedIndividualAccount]](any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              APIDetailedIndividualAccount(
                2,
                "ggEId12",
                APIIndividualDetails(
                  9876,
                  "anotherFirstName",
                  "anotherLastName",
                  "theFakeDonald@potus.com",
                  Some("24680"),
                  Some("13579"),
                  "idv1"
                ),
                13579,
                GroupDetails(
                  345,
                  false,
                  "Fake News Inc",
                  "therealdonald@potus.com",
                  Some("9876541")
                )
              )
            )))

      testConnector.getDetailedIndividual(id)(hc).futureValue shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided id cannot be found" in {
      val id = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedIndividualAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getDetailedIndividual(id)(hc).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.findDetailedIndividualAccountByGGID" should {
    "return the individual account associated with the provided GGID" in {
      val ggId = "1234"

      when(defaultHttpClient.GET[Option[APIDetailedIndividualAccount]](any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              APIDetailedIndividualAccount(
                2,
                "ggEId12",
                APIIndividualDetails(
                  9876,
                  "anotherFirstName",
                  "anotherLastName",
                  "theFakeDonald@potus.com",
                  Some("24680"),
                  Some("13579"),
                  "idv1"
                ),
                13579,
                GroupDetails(
                  345,
                  false,
                  "Fake News Inc",
                  "therealdonald@potus.com",
                  Some("9876541")
                )
              )
            )))

      testConnector.findDetailedIndividualAccountByGGID(ggId)(hc).futureValue shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"

      when(defaultHttpClient.GET[Option[APIDetailedIndividualAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.findDetailedIndividualAccountByGGID(ggId)(hc).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.createIndividualAccount" should {
    "return an individual account id for the individual account submission" in {
      when(
        defaultHttpClient
          .POST[APIIndividualAccount, IndividualAccountId](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(IndividualAccountId(12345)))

      testConnector
        .createIndividualAccount(individualAccountSubmission)(hc)
        .futureValue shouldBe expectedCreateResponseValid
    }
  }

  "CustomerManagementApi.updateIndividualAccount" should {
    "update the person id with the individual account submission" in {
      val personId = 1234L
      val updateUrl = s"$url/$personId"

      when(defaultHttpClient.PUT[APIIndividualAccount, JsValue](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(Json.toJson(APIDetailedIndividualAccount(
          2,
          "ggEId12",
          APIIndividualDetails(
            9876,
            "anotherFirstName",
            "anotherLastName",
            "theFakeDonald@potus.com",
            Some("24680"),
            Some("13579"),
            "idv1"
          ),
          13579,
          GroupDetails(
            345,
            false,
            "Fake News Inc",
            "therealdonald@potus.com",
            Some("9876541")
          )
        ))))

      testConnector
        .updateIndividualAccount(personId, individualAccountSubmission)(hc)
        .futureValue shouldBe expectedUpdateValidResponse
    }
  }

  "CustomerManagementApi.getDetailedGroupAccount" should {
    "return the group accounts associated with the provided id" in {
      val groupId = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              APIDetailedGroupAccount(
                2,
                "gggId",
                Some(234L),
                GroupDetails(
                  345,
                  false,
                  "Fake News Inc",
                  "therealdonald@potus.com",
                  Some("9876541")
                ),
                Seq()
              )
            )))

      testConnector.getDetailedGroupAccount(groupId)(hc).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided id cannot be found" in {
      val groupId = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getDetailedGroupAccount(groupId)(hc).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.findDetailedGroupAccountByGGID" should {
    "return the group accounts associated with the provided GGID" in {
      val ggId = "1234"

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              APIDetailedGroupAccount(
                2,
                "gggId",
                Some(234L),
                GroupDetails(
                  345,
                  false,
                  "Fake News Inc",
                  "therealdonald@potus.com",
                  Some("9876541")
                ),
                Seq()
              )
            )))

      testConnector.findDetailedGroupAccountByGGID(ggId)(hc).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.findDetailedGroupAccountByGGID(ggId)(hc).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.withAgentCode" should {
    "return the group accounts associated with the provided agent code" in {
      val agentCode = "ac234"

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(
          Future.successful(
            Some(
              APIDetailedGroupAccount(
                2,
                "gggId",
                Some(234L),
                GroupDetails(
                  345,
                  false,
                  "Fake News Inc",
                  "therealdonald@potus.com",
                  Some("9876541")
                ),
                Seq()
              )
            )))

      testConnector.withAgentCode(agentCode)(hc).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided agent code cannot be found" in {
      val agentCode = "ac234"

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.withAgentCode(agentCode)(hc).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.createGroupAccount" should {
    "return the created account's group id" in {

      when(defaultHttpClient.POST[APIGroupAccountSubmission, GroupId](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(models.GroupId(654321, "valid group id", 45678)))

      testConnector.createGroupAccount(groupAccountSubmission)(hc).futureValue shouldBe groupId
    }
  }

  "CustomerManagementApi.updateGroupAccount" should {
    "return unit after updating the account" in {

      val orgId = 123456789
      val updatedOrgAccount = UpdatedOrganisationAccount(
        governmentGatewayGroupId = "gggId1",
        addressUnitId = 1234567,
        representativeFlag = true,
        organisationName = "Fake News Inc",
        organisationEmailAddress = "therealdonald@whitehouse.com",
        organisationTelephoneNumber = "0987612345",
        effectiveFrom = instant,
        changedByGGExternalId = "tester1"
      )

      when(
        defaultHttpClient
          .PUT[UpdatedOrganisationAccount, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      val result: Unit = testConnector.updateGroupAccount(orgId = orgId, updatedOrgAccount).futureValue
      result should be(())
    }
  }

  private lazy val expectedCreateResponseValid = IndividualAccountId(12345)
  "CustomerManagementApi.getAgentByRepresentationCode" should {
    "return AgentOrganisation with the provided agentCode" in {
      val agentCode = 123432L

      when(defaultHttpClient.GET[Option[AgentOrganisation]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(agentOrganisation)))

      testConnector.getAgentByRepresentationCode(agentCode)(hc).futureValue shouldBe Some(agentOrganisation)
    }

    "return an None if no AgentOrgaisation can be found for the provided agentCode" in {
      val agentCode = 123432L

      when(defaultHttpClient.GET[Option[AgentOrganisation]](any(), any())(any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getAgentByRepresentationCode(agentCode)(hc).futureValue shouldBe None
    }
  }
}
