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

import java.time._

import basespecs.BaseUnitSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CustomerManagementApiSpec extends BaseUnitSpec {

  val defaultHttpClient = mock[DefaultHttpClient]
  val config = mockServicesConfig
  val testConnector = new CustomerManagementApi(defaultHttpClient, config) {
    override lazy val baseUrl = "http://some-uri"
  }

  val url = s"/customer-management-api/person"

  "CustomerManagementApi.getDetailedIndividual" should {
    "return the individual account associated with the provided person id" in {
      val id = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedIndividualAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
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
        .thenReturn(Future.successful(Some(
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
      when(defaultHttpClient.POST[APIIndividualAccount, IndividualAccountId](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(IndividualAccountId(12345)))

      testConnector.createIndividualAccount(individualAccountSubmission)(hc).futureValue shouldBe expectedCreateResponseValid
    }
  }

  "CustomerManagementApi.updateIndividualAccount" should {
    "update the person id with the individual account submission" in {
      val personId = 1234L
      val updateUrl = s"$url/$personId"

      when(defaultHttpClient.PUT[APIIndividualAccount, JsValue](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(
          Json.toJson(APIDetailedIndividualAccount(
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

      testConnector.updateIndividualAccount(personId, individualAccountSubmission)(hc).futureValue shouldBe expectedUpdateValidResponse
    }
  }

  "CustomerManagementApi.getDetailedGroupAccount" should {
    "return the group accounts associated with the provided id" in {
      val groupId = 1234L

      when(defaultHttpClient.GET[Option[APIDetailedGroupAccount]](any())(any(), any(), any()))
        .thenReturn(Future.successful(Some(
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

      testConnector.getDetailedGroupAccount(groupId)(hc).futureValue shouldBe expectedGetValidResponse1
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
        .thenReturn(Future.successful(Some(
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


     testConnector.findDetailedGroupAccountByGGID(ggId)(hc).futureValue shouldBe expectedGetValidResponse1
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
        .thenReturn(Future.successful(Some(
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

      testConnector.withAgentCode(agentCode)(hc).futureValue shouldBe expectedGetValidResponse1
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

      testConnector.createGroupAccount(createValidRequest)(hc).futureValue shouldBe expectedCreateValidResponse
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

      when(defaultHttpClient.PUT[UpdatedOrganisationAccount, HttpResponse](any(), any())(any(), any(), any(), any()))
        .thenReturn(Future.successful(HttpResponse(200)))

      val result: Unit = testConnector.updateGroupAccount(orgId = orgId, updatedOrgAccount).futureValue
      result should be (())
    }
  }

  private lazy val expectedCreateResponseValid =  IndividualAccountId(12345)

  private lazy val individualAccountSubmission = IndividualAccountSubmission(
    externalId = "ggEId12",
    trustId = "idv1",
    organisationId = 13579,
    details = IndividualDetails(
      firstName  = "Kim",
      lastName = "Yong Un",
      email = "thechosenone@nkorea.nk",
      phone1 = "24680",
      phone2 = Some("13579"),
      addressId = 9876
    )
  )

  private lazy val expectedGetValidResponse = Some(IndividualAccount(
    externalId = "ggEId12",
    trustId = "idv1",
    organisationId = 13579,
    individualId = 2,
    details = IndividualDetails(
      firstName="anotherFirstName",
      lastName="anotherLastName",
      email= "theFakeDonald@potus.com",
      phone1= "24680",
      phone2= Some("13579"),
      addressId= 9876
    )
  )
  )

  private val expectedUpdateValidResponse = Json.parse("""{
    "id": 2,
    "governmentGatewayExternalId": "ggEId12",
    "personLatestDetail": {
      |"addressUnitId": 9876,
      |"firstName": "anotherFirstName",
      |"lastName": "anotherLastName",
      |"emailAddress": "theFakeDonald@potus.com",
      |"telephoneNumber": "24680",
      |"mobileNumber": "13579",
      |"identifyVerificationId": "idv1"
    },
    "organisationId": 13579,
    "organisationLatestDetail": {
      "addressUnitId": 345,
      "representativeFlag": false,
      "organisationName": "Fake News Inc",
      "organisationEmailAddress": "therealdonald@potus.com",
      "organisationTelephoneNumber": "9876541"
      }
  }""".stripMargin)


  private lazy val expectedGetEmptyResponse = None

  implicit lazy val fixedClock: Clock = Clock.fixed(Instant.now, ZoneId.systemDefault())

  private lazy val createValidRequest: GroupAccountSubmission = GroupAccountSubmission(
    id = "acc123",
    companyName = "Real news Inc",
    addressId = 9876543L,
    email = "thewhitehouse@potus.com",
    phone = "01987654",
    isAgent = false,
    individualAccountSubmission = IndividualAccountSubmissionForOrganisation(
      externalId = "Ext123",
      trustId= "trust234",
      details = IndividualDetails(
        firstName = "Donald",
        lastName = "Trump",
        email = "therealdonald@potus.com",
        phone1= "123456789",
        phone2= Some("987654321"),
        addressId= 24680L
      )
    )
  )

  private lazy val expectedCreateValidResponse = GroupId(
    id = 654321L,
    message="valid group id",
    responseTime = 45678)

  private lazy val expectedGetValidResponse1 = Some(GroupAccount(
    id = 2,
    groupId = "gggId",
    companyName = "Fake News Inc",
    addressId=345,
    email="therealdonald@potus.com",
    phone = "9876541",
    isAgent = false,
    agentCode = None)
  )

}
