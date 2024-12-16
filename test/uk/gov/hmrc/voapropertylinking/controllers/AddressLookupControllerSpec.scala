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

package uk.gov.hmrc.voapropertylinking.controllers

import basespecs.BaseControllerSpec
import models.modernised.addressmanagement.{DetailedAddress, SimpleAddress}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsDefined, JsNumber, JsSuccess, Json}
import play.api.mvc.Result
import play.api.test.Helpers

import scala.concurrent.Future

class AddressLookupControllerSpec extends BaseControllerSpec {

  trait Setup {
    val controller = new AddressLookupController(
      Helpers.stubControllerComponents(),
      preAuthenticatedActionBuilders(),
      mockModernisedAddressManagementApi,
      mockAddressManagementApi,
      mockFeatureSwitch
    )

    val validPostcode = "BN1 1NB"
    val addressUnitId = 1L
    val detailedAddress: DetailedAddress = DetailedAddress(
      addressUnitId = Some(1L),
      nonAbpAddressId = Some(2L),
      organisationName = Some("Org name"),
      departmentName = Some("Dept name"),
      subBuildingName = Some("subBuildingName"),
      buildingName = Some("buildingName"),
      buildingNumber = Some("buildingNumber"),
      dependentThoroughfareName = Some("dependentThoroughfareName"),
      thoroughfareName = Some("thoroughfareName"),
      doubleDependentLocality = Some("doubleDependentLocality"),
      dependentLocality = Some("dependentLocality"),
      postTown = "postTown",
      postcode = validPostcode
    )
    val simpleAddress: SimpleAddress = detailedAddress.simplify
  }

  "Using the controller with the bstDownstream feature switch enabled" when {
    "looking up an address by postcode" should {
      "return 200 OK with addresses retrieved via the connector" when {
        "the post code is a valid string" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockAddressManagementApi.find(mEq(validPostcode))(any()))
            .thenReturn(Future.successful(Seq(detailedAddress)))

          val result: Future[Result] =
            controller.find(validPostcode)(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[Seq[DetailedAddress]]) {
            case JsSuccess(addresses, _) =>
              addresses.loneElement shouldBe detailedAddress
          }
        }
      }

      "return 400 BAD REQUEST" when {
        "an invalid post code format is used" in new Setup {
          val result: Future[Result] = controller.find("this is not a valid postcode")(request)

          status(result) shouldBe BAD_REQUEST
        }
      }
    }

    "getting an address by unit id" should {
      "return 200 OK" when {
        "an address is found in modernised" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockAddressManagementApi.get(mEq(addressUnitId))(any()))
            .thenReturn(Future.successful(Some(simpleAddress)))

          val result: Future[Result] =
            controller.get(addressUnitId)(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[SimpleAddress]) {
            case JsSuccess(a, _) => a shouldBe simpleAddress
          }
        }
      }

      "return 404 NOT FOUND" when {
        "the address doesn't exist" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockAddressManagementApi.get(mEq(addressUnitId))(any()))
            .thenReturn(Future.successful(Option.empty[SimpleAddress]))

          val result: Future[Result] =
            controller.get(addressUnitId)(request)

          status(result) shouldBe NOT_FOUND
        }
      }
    }

    "creating a simple address" should {
      "return 201 CREATED" when {
        "valid JSON SimpleAddress is submitted" in new Setup {
          when(mockFeatureSwitch.isBstDownstreamEnabled).thenReturn(true)
          when(mockAddressManagementApi.create(mEq(simpleAddress))(any()))
            .thenReturn(Future.successful(123L))

          val result: Future[Result] =
            controller.create(request.withBody(toJson(simpleAddress)))

          status(result) shouldBe CREATED
          inside(contentAsJson(result) \ "id") {
            case JsDefined(JsNumber(value)) => value shouldBe 123L
          }
        }
      }
      "reject invalid payload with 400 BAD REQUEST" when {
        "submitted JSON isn't a valid SimpleAddress" in new Setup {
          val result: Future[Result] =
            controller.create(request.withBody(Json.parse("""{"foo": "bar"}""")))

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }

  "Using the controller with the bstDownstream feature switch disabled" when {
    "looking up an address by postcode" should {
      "return 200 OK with addresses retrieved via the connector" when {
        "the post code is a valid string" in new Setup {
          when(mockModernisedAddressManagementApi.find(mEq(validPostcode))(any()))
            .thenReturn(Future.successful(Seq(detailedAddress)))

          val result: Future[Result] =
            controller.find(validPostcode)(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[Seq[DetailedAddress]]) {
            case JsSuccess(addresses, _) =>
              addresses.loneElement shouldBe detailedAddress
          }
        }
      }

      "return 400 BAD REQUEST" when {
        "an invalid post code format is used" in new Setup {
          val result: Future[Result] =
            controller.find("this is not a valid postcode")(request)

          status(result) shouldBe BAD_REQUEST
          verify(mockModernisedAddressManagementApi, never()).find(any())(any())
        }
      }
    }

    "getting an address by unit id" should {
      "return 200 OK" when {
        "an address is found in modernised" in new Setup {
          when(mockModernisedAddressManagementApi.get(mEq(addressUnitId))(any()))
            .thenReturn(Future.successful(Some(simpleAddress)))

          val result: Future[Result] =
            controller.get(addressUnitId)(request)

          status(result) shouldBe OK
          inside(contentAsJson(result).validate[SimpleAddress]) {
            case JsSuccess(a, _) => a shouldBe simpleAddress
          }
        }
      }

      "return 404 NOT FOUND" when {
        "the address doesn't exist" in new Setup {
          when(mockModernisedAddressManagementApi.get(mEq(addressUnitId))(any()))
            .thenReturn(Future.successful(Option.empty[SimpleAddress]))

          val result: Future[Result] =
            controller.get(addressUnitId)(request)

          status(result) shouldBe NOT_FOUND
        }
      }
    }

    "creating a simple address" should {
      "return 201 CREATED" when {
        "valid JSON SimpleAddress is submitted" in new Setup {
          when(mockModernisedAddressManagementApi.create(mEq(simpleAddress))(any()))
            .thenReturn(Future.successful(123L))

          val result: Future[Result] =
            controller.create(request.withBody(toJson(simpleAddress)))

          status(result) shouldBe CREATED
          inside(contentAsJson(result) \ "id") {
            case JsDefined(JsNumber(value)) => value shouldBe 123L
          }
        }
      }
      "reject invalid payload with 400 BAD REQUEST" when {
        "submitted JSON isn't a valid SimpleAddress" in new Setup {
          val result: Future[Result] =
            controller.create(request.withBody(Json.parse("""{"foo": "bar"}""")))

          status(result) shouldBe BAD_REQUEST
        }
      }
    }
  }
}
