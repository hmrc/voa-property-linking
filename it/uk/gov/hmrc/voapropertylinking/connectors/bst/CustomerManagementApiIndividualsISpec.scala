package uk.gov.hmrc.voapropertylinking.connectors.bst

import models.{IndividualAccount, IndividualAccountId, IndividualAccountSubmission, IndividualDetails}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.stubs.bst.CustomerManagementStub
import utils.FakeObjects

import java.time.Instant
import scala.concurrent.ExecutionContext

class CustomerManagementApiIndividualsISpec
  extends BaseIntegrationSpec with CustomerManagementStub with FakeObjects {

  trait TestSetup {
    lazy val connector: CustomerManagementApi = app.injector.instanceOf[CustomerManagementApi]
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
  }

  "createIndividualAccount" should {
    val timeString = "2023-03-29T16:31:17.050540Z"
    val requestJson: JsValue =
      Json.parse(s"""
                    |{
                    |  "personData" : {
                    |    "identifyVerificationId" : "idv1",
                    |    "firstName" : "Kim",
                    |    "lastName" : "Yong Un",
                    |    "organisationId" : 13579,
                    |    "addressUnitId" : 9876,
                    |    "telephoneNumber" : "24680",
                    |    "mobileNumber" : "13579",
                    |    "emailAddress" : "thechosenone@nkorea.nk",
                    |    "governmentGatewayExternalId" : "ggEId12",
                    |    "effectiveFrom" : "$timeString"
                    |  }
                    |}
                    |""".stripMargin.trim)
    val account: IndividualAccountSubmission = individualAccountSubmission

    "return a valid IndividualAccountId" in new TestSetup {
      val expectedResponse = IndividualAccountId(id = 12345)
      val responseJson: JsObject = Json.obj("id" -> 12345)

      stubCreateIndividualAccount(requestJson)(OK, responseJson)

      val result: IndividualAccountId = await(connector.createIndividualAccount(account, Instant.parse(timeString)))

      result shouldBe expectedResponse
    }
    "return an exception" when {
      "it receives a downstream 4xx response" in new TestSetup {
        stubCreateIndividualAccount(requestJson)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.createIndividualAccount(account, Instant.parse(timeString)))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
      "it receives a downstream 5xx response" in new TestSetup {
        stubCreateIndividualAccount(requestJson)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.createIndividualAccount(account, Instant.parse(timeString)))
        }

        result shouldBe a[UpstreamErrorResponse]
      }
    }
  }

  "updateIndividualAccount" should {
    val timeString = "2023-03-29T16:31:17.050540Z"
    val requestJson: JsValue =
      Json.parse(
        s"""
           |{
           |  "personData" : {
           |    "identifyVerificationId" : "idv1",
           |    "firstName" : "Kim",
           |    "lastName" : "Yong Un",
           |    "organisationId" : 13579,
           |    "addressUnitId" : 9876,
           |    "telephoneNumber" : "24680",
           |    "mobileNumber" : "13579",
           |    "emailAddress" : "thechosenone@nkorea.nk",
           |    "governmentGatewayExternalId" : "ggEId12",
           |    "effectiveFrom" : "$timeString"
           |  }
           |}
           |""".stripMargin.trim)

    val expectedResponseJson: JsValue =
      Json.parse(
        """
          |{
          |  "id": 2,
          |  "governmentGatewayExternalId": "ggEId12",
          |  "personLatestDetail": {
          |    "addressUnitId": 9876,
          |    "firstName": "anotherFirstName",
          |    "lastName": "anotherLastName",
          |    "emailAddress": "theFakeDonald@potus.com",
          |    "telephoneNumber": "24680",
          |    "mobileNumber": "13579",
          |    "identifyVerificationId": "idv1"
          |  },
          |  "organisationId": 13579,
          |  "organisationLatestDetail": {
          |    "addressUnitId": 345,
          |    "representativeFlag": false,
          |    "organisationName": "Fake News Inc",
          |    "organisationEmailAddress": "therealdonald@potus.com",
          |    "organisationTelephoneNumber": "9876541"
          |  }
          |}
      """.stripMargin)
    val account: IndividualAccountSubmission = individualAccountSubmission

    "return the JsValue on success, but actually returns a unit" in new TestSetup {
      val anyPersonId = 123456L
      stubUpdateIndividualAccount(anyPersonId, requestJson)(OK, expectedResponseJson)

      val result: Unit = await(connector.updateIndividualAccount(anyPersonId, account, Instant.parse(timeString)))

      result shouldBe ()
    }
    "return an exception" when {
      "it receives a downstream 4xx response" in new TestSetup {
        val anyPersonId = 123456L
        stubUpdateIndividualAccount(anyPersonId, requestJson)(BAD_REQUEST, Json.obj("any" -> "error"))

        val result: Exception = intercept[Exception] {
          await(connector.updateIndividualAccount(anyPersonId, account, Instant.parse(timeString)))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
      "it receives a downstream 5xx response" in new TestSetup {
        val anyPersonId = 123456L
        stubUpdateIndividualAccount(anyPersonId, requestJson)(INTERNAL_SERVER_ERROR, Json.obj("any" -> "error"))

        val result: Exception = intercept[Exception] {
          await(connector.updateIndividualAccount(anyPersonId, account, Instant.parse(timeString)))
        }

        result shouldBe an [UpstreamErrorResponse]
      }
    }
  }

  "getDetailedIndividual" should {

    val personId: Long = 123456L

    "return an IndividualAccount when a valid JSON body is returned" should {
      "it receives a successful response" in new TestSetup {
        val responseJson: JsValue =
          Json.parse(
            s"""
               |{
               |  "id": $personId,
               |  "governmentGatewayExternalId": "ggEId12",
               |  "personLatestDetail": {
               |    "addressUnitId": 9876,
               |    "firstName": "anotherFirstName",
               |    "lastName": "anotherLastName",
               |    "emailAddress": "theFakeDonald@potus.com",
               |    "telephoneNumber": "24680",
               |    "mobileNumber": "13579",
               |    "identifyVerificationId": "idv1"
               |  },
               |  "organisationId": 13579,
               |  "organisationLatestDetail": {
               |    "addressUnitId": 345,
               |    "representativeFlag": false,
               |    "organisationName": "Fake News Inc",
               |    "organisationEmailAddress": "therealdonald@potus.com",
               |    "organisationTelephoneNumber": "9876541"
               |  }
               |}
               |""".stripMargin)

        val expectedIndividualAccount = IndividualAccount(
          externalId = "ggEId12",
          trustId = Some("idv1"),
          organisationId = 13579,
          individualId = personId,
          details = IndividualDetails(
            firstName = "anotherFirstName",
            lastName = "anotherLastName",
            email = "theFakeDonald@potus.com",
            phone1 = "24680",
            phone2 = Some("13579"),
            addressId = 9876
          )
        )

        stubGetDetailedIndividual(personId)(OK, responseJson)

        val result: Option[IndividualAccount] = await(connector.getDetailedIndividual(personId))

        result shouldBe Some(expectedIndividualAccount)
      }
    }
    "throw an exception" when {
      "it receives a 2xx with an invalid body response" in new TestSetup {
        stubGetDetailedIndividual(personId)(OK, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedIndividual(personId))
        }

        result shouldBe a[JsValidationException]
      }
      "it receives a 4xx response" in new TestSetup {
        stubGetDetailedIndividual(personId)(BAD_REQUEST, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedIndividual(personId))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
      "it receives a 5xx response" in new TestSetup {
        stubGetDetailedIndividual(personId)(INTERNAL_SERVER_ERROR, Json.obj())

        val result: Exception = intercept[Exception] {
          await(connector.getDetailedIndividual(personId))
        }

        result shouldBe an[UpstreamErrorResponse]
      }
    }
  }

    "findDetailedIndividualAccountByGGID" should {
      val ggId = "gggId"
      "return a GroupAccount when a valid JSON body is returned" should {
        "it receives a successful response" in new TestSetup {
          val responseJson: JsValue =
            Json.parse(
              s"""
                 |{
                 |  "id": 123456,
                 |  "governmentGatewayExternalId": "$ggId",
                 |  "personLatestDetail": {
                 |    "addressUnitId": 9876,
                 |    "firstName": "anotherFirstName",
                 |    "lastName": "anotherLastName",
                 |    "emailAddress": "theFakeDonald@potus.com",
                 |    "telephoneNumber": "24680",
                 |    "mobileNumber": "13579",
                 |    "identifyVerificationId": "idv1"
                 |  },
                 |  "organisationId": 13579,
                 |  "organisationLatestDetail": {
                 |    "addressUnitId": 345,
                 |    "representativeFlag": false,
                 |    "organisationName": "Fake News Inc",
                 |    "organisationEmailAddress": "therealdonald@potus.com",
                 |    "organisationTelephoneNumber": "9876541"
                 |  }
                 |}
                 |""".stripMargin)
          val expectedIndividualAccount = IndividualAccount(
            externalId = ggId,
            trustId = Some("idv1"),
            organisationId = 13579,
            individualId = 123456L,
            details = IndividualDetails(
              firstName = "anotherFirstName",
              lastName = "anotherLastName",
              email = "theFakeDonald@potus.com",
              phone1 = "24680",
              phone2 = Some("13579"),
              addressId = 9876
            )
          )

          stubFindDetailedIndividualAccountByGGID(ggId)(OK, responseJson)

          val result: Option[IndividualAccount] = await(connector.findDetailedIndividualAccountByGGID(ggId))

          result shouldBe Some(expectedIndividualAccount)
        }
      }
      "throw an exception" when {
        "it receives a 2xx with an invalid body response" in new TestSetup {
          stubFindDetailedIndividualAccountByGGID(ggId)(OK, Json.obj())

          val result: Exception = intercept[Exception] {
            await(connector.findDetailedIndividualAccountByGGID(ggId))
          }

          result shouldBe a[JsValidationException]
        }
        "it receives a 4xx response" in new TestSetup {
          stubFindDetailedIndividualAccountByGGID(ggId)(BAD_REQUEST, Json.obj())

          val result: Exception = intercept[Exception] {
            await(connector.findDetailedIndividualAccountByGGID(ggId))
          }

          result shouldBe an[UpstreamErrorResponse]
        }
        "it receives a 5xx response" in new TestSetup {
          stubFindDetailedIndividualAccountByGGID(ggId)(INTERNAL_SERVER_ERROR, Json.obj())

          val result: Exception = intercept[Exception] {
            await(connector.findDetailedIndividualAccountByGGID(ggId))
          }

          result shouldBe an[UpstreamErrorResponse]
        }
      }
    }

}
