package uk.gov.hmrc.voapropertylinking.connectors.bst

import models.modernised.addressmanagement.{DetailedAddress, SimpleAddress}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.await
import uk.gov.hmrc.http.{HeaderCarrier, JsValidationException, UpstreamErrorResponse}
import uk.gov.hmrc.voapropertylinking.BaseIntegrationSpec
import uk.gov.hmrc.voapropertylinking.connectors.errorhandler.VoaClientException
import uk.gov.hmrc.voapropertylinking.stubs.bst.AddressManagementStub
import uk.gov.hmrc.voapropertylinking.utils.HttpStatusCodes.{NOT_FOUND, OK}

import scala.concurrent.ExecutionContext

class AddressManagementApiISpec extends BaseIntegrationSpec with AddressManagementStub {

  trait TestSetup {
    lazy val connector: AddressManagementApi = app.injector.instanceOf[AddressManagementApi]
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrier()
  }

  val addressUnitId = 1234L
  val postcode = "L4 0TH"
  val simpleAddress: SimpleAddress = SimpleAddress(
    addressUnitId = Some(addressUnitId),
    line1 = "Liverpool FC, First team",
    line2 = "Anfield Stadium",
    line3 = "Anfield Road",
    line4 = "Liverpool",
    postcode = postcode
  )
  val detailedAddress: DetailedAddress = DetailedAddress(
    addressUnitId = Some(addressUnitId),
    nonAbpAddressId = Some(1234),
    organisationName = Some("Liverpool FC"),
    departmentName = Some("First team"),
    buildingName = Some("Anfield Stadium"),
    dependentThoroughfareName = Some("Anfield Road"),
    postTown = "Liverpool",
    postcode = postcode
  )
  val detailedAddressJson: JsObject = Json.obj(
    "addressUnitId" -> addressUnitId,
    "nonAbpAddressId" -> 1234,
    "organisationName" -> "Liverpool FC",
    "departmentName" -> "First team",
    "buildingName" -> "Anfield Stadium",
    "dependentThoroughfareName" -> "Anfield Road",
    "postTown" -> "Liverpool",
    "postcode" -> s"$postcode"
  )

  "find" should {
    "return the correct data model" when {
      "supplied the correct data" in new TestSetup {
        val responseJson: JsObject = Json.obj("addressDetails" -> Json.arr(detailedAddressJson))
        stubFind(postcode)(OK, responseJson)

        val result: Seq[DetailedAddress] = await(connector.find(postcode))

        result shouldBe Seq(detailedAddress)
      }
    }
    "return an empty list" when {
      "returned an empty list" in new TestSetup {
        val responseJson: JsObject = Json.obj("addressDetails" -> Json.arr())
        stubFind(postcode)(OK, responseJson)

        val result: Seq[DetailedAddress] = await(connector.find(postcode))

        result shouldBe Seq.empty[DetailedAddress]
      }
    }
    "throw an exception" when {
      "invalid json is returned" in new TestSetup {
        val responseJson: JsObject = Json.obj("invalidJson" -> "returned")
        stubFind(postcode)(OK, responseJson)

        assertThrows[JsValidationException] {
          await(connector.find(postcode))
        }
      }
      "it receives an error from the http request " in new TestSetup {
        val responseJson: JsObject = Json.obj("doesnt" -> "matter")
        stubFind(postcode)(NOT_FOUND, responseJson)

        assertThrows[UpstreamErrorResponse] {
          await(connector.find(postcode))
        }
      }
    }
  }

  "get" should {
    "return the correct optional data model" when {
      "it receives the correct data from the downstream" in new TestSetup {
        val responseJson: JsObject = Json.obj("addressDetails" -> Json.arr(detailedAddressJson))
        stubGet(addressUnitId)(OK, responseJson)

        val result: Option[SimpleAddress] = await(connector.get(addressUnitId))
        result shouldBe Some(simpleAddress)
      }
    }
    "return a questionable SimpleAddress model" when {
      "the detailed model only returns non-optional fields" in new TestSetup {
        val responseJson: JsObject = Json.obj(
          "addressDetails" -> Json.arr(
            Json.obj("postTown" -> "Liverpool", "postcode" -> postcode)
          )
        )
        stubGet(addressUnitId)(OK, responseJson)

        val questionableSimplifiedAddress: SimpleAddress =
          SimpleAddress(
            addressUnitId = None,
            line1 = "",
            line2 = "",
            line3 = "",
            line4 = "Liverpool",
            postcode = postcode
          )

        val result: Option[SimpleAddress] = await(connector.get(addressUnitId))

        result shouldBe Some(questionableSimplifiedAddress)
      }
    }
    "return 'None'" when {
      "it receives an empty 'addressDetails' json array" in new TestSetup {
        val responseJson: JsObject = Json.obj("addressDetails" -> Json.arr())
        stubGet(addressUnitId)(OK, responseJson)

        val result: Option[SimpleAddress] = await(connector.get(addressUnitId))
        result shouldBe None
      }
      "it receives json response that can't be parsed to an 'Address'" in new TestSetup {
        val responseJson: JsObject = Json.obj("notAddressDetails" -> Json.obj())
        stubGet(addressUnitId)(OK, responseJson)

        val result: Option[SimpleAddress] = await(connector.get(addressUnitId))
        result shouldBe None
      }
      "it receives an error from the http request" in new TestSetup {
        val responseJson: JsObject = Json.obj("doesnt" -> "matter")
        stubGet(addressUnitId)(NOT_FOUND, responseJson)

        val result: Option[SimpleAddress] = await(connector.get(addressUnitId))
        result shouldBe None
      }
    }
  }

  "create" should {
    "return the correct data model" when {
      "supplied the correct data" in new TestSetup {
        val responseId: Long = 54321L
        val responseJson: JsObject = Json.obj("id" -> responseId)
        stubCreate(simpleAddress.toDetailedAddress)(OK, responseJson)

        val result: Long = await(connector.create(simpleAddress))

        result shouldBe responseId
      }
    }
    "throw an exception" when {
      "the json response body is invalid" in new TestSetup {
        val responseJson: JsObject = Json.obj("not" -> "valid")
        stubCreate(simpleAddress.toDetailedAddress)(OK, responseJson)

        val result: Exception = intercept[Exception] {
          await(connector.create(simpleAddress))
        }

        result.getMessage shouldBe s"Failed to create record for address $simpleAddress"
      }
      "the response status is anything but 200 (OK)" in new TestSetup {
        val responseId: Long = 54321L
        val responseJson: JsObject = Json.obj("id" -> responseId)
        stubCreate(simpleAddress.toDetailedAddress)(NOT_FOUND, responseJson)

        val result: Exception = intercept[Exception] {
          await(connector.create(simpleAddress))
        }

        result.getMessage shouldBe s"$responseJson"
      }
    }
  }
}
