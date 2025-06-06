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
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.voapropertylinking.http.VoaHttpClient

import scala.concurrent.Future

class ModernisedCustomerManagementApiSpec extends BaseUnitSpec {

  val defaultHttpClient: VoaHttpClient = mock[VoaHttpClient]
  val config: ServicesConfig = mockServicesConfig
  val testConnector: ModernisedCustomerManagementApi =
    new ModernisedCustomerManagementApi(defaultHttpClient, mockAppConfig) {
      override lazy val baseUrl = "http://some-uri"
    }

  val url = s"/customer-management-api/person"

  "CustomerManagementApi.getDetailedIndividual" should {
    "return the individual account associated with the provided person id" in {
      val id = 1234L

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedIndividualAccount]](any())(any(), any(), any(), any()))
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
                  Some("idv1")
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
            )
          )
        )

      testConnector.getDetailedIndividual(id)(requestWithPrincipal).futureValue shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided id cannot be found" in {
      val id = 1234L

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedIndividualAccount]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getDetailedIndividual(id)(requestWithPrincipal).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.findDetailedIndividualAccountByGGID" should {
    "return the individual account associated with the provided GGID" in {
      val ggId = "1234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedIndividualAccount]](any())(any(), any(), any(), any()))
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
                  Some("idv1")
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
            )
          )
        )

      testConnector
        .findDetailedIndividualAccountByGGID(ggId)(requestWithPrincipal)
        .futureValue shouldBe expectedGetValidResponse
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedIndividualAccount]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector
        .findDetailedIndividualAccountByGGID(ggId)(requestWithPrincipal)
        .futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.createIndividualAccount" should {
    "return an individual account id for the individual account submission" in {
      when(
        defaultHttpClient
          .postWithGgHeaders[IndividualAccountId](any(), any())(any(), any(), any(), any())
      ).thenReturn(Future.successful(IndividualAccountId(12345)))

      testConnector
        .createIndividualAccount(individualAccountSubmission)(requestWithPrincipal)
        .futureValue shouldBe expectedCreateResponseValid
    }
  }

  "CustomerManagementApi.updateIndividualAccount" should {
    "update the person id with the individual account submission" in {
      val personId = 1234L

      when(defaultHttpClient.putWithGgHeaders[JsValue](any(), any())(any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            Json.toJson(
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
                  Some("idv1")
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
            )
          )
        )

      testConnector
        .updateIndividualAccount(personId, individualAccountSubmission)(requestWithPrincipal)
        .futureValue shouldBe expectedUpdateValidResponse
    }
  }

  "CustomerManagementApi.getDetailedGroupAccount" should {
    "return the group accounts associated with the provided id" in {
      val groupId = 1234L

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
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
            )
          )
        )

      testConnector.getDetailedGroupAccount(groupId)(requestWithPrincipal).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided id cannot be found" in {
      val groupId = 1234L

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.getDetailedGroupAccount(groupId)(requestWithPrincipal).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.findDetailedGroupAccountByGGID" should {
    "return the group accounts associated with the provided GGID" in {
      val ggId = "1234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
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
            )
          )
        )

      testConnector.findDetailedGroupAccountByGGID(ggId)(requestWithPrincipal).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided GGID cannot be found" in {
      val ggId = "1234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector
        .findDetailedGroupAccountByGGID(ggId)(requestWithPrincipal)
        .futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.withAgentCode" should {
    "return the group accounts associated with the provided agent code" in {
      val agentCode = "ac234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
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
            )
          )
        )

      testConnector.withAgentCode(agentCode)(requestWithPrincipal).futureValue shouldBe someGroupAccount
    }

    "return an empty response if the provided agent code cannot be found" in {
      val agentCode = "ac234"

      when(defaultHttpClient.getWithGGHeaders[Option[APIDetailedGroupAccount]](any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(None))

      testConnector.withAgentCode(agentCode)(requestWithPrincipal).futureValue shouldBe expectedGetEmptyResponse
    }
  }

  "CustomerManagementApi.createGroupAccount" should {
    "return the created account's group id" in {

      when(defaultHttpClient.postWithGgHeaders[GroupId](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(models.GroupId(654321, "valid group id", 45678)))

      testConnector.createGroupAccount(groupAccountSubmission)(requestWithPrincipal).futureValue shouldBe groupId
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
          .putWithGgHeaders[HttpResponse](any(), any())(any(), any(), any(), any())
      ).thenReturn(Future.successful(emptyJsonHttpResponse(200)))

      val result: Unit = testConnector.updateGroupAccount(orgId = orgId, updatedOrgAccount).futureValue
      result should be(())
    }
  }

  private lazy val expectedCreateResponseValid = IndividualAccountId(12345)
}
