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
import models.modernised.addressmanagement.{Addresses, DetailedAddress, SimpleAddress}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.Future

class ModernisedAddressManagementApiSpec extends BaseUnitSpec {

  trait Setup {
    val connector = new ModernisedAddressManagementApi(mockDefaultHttpClient, mockServicesConfig)
    val url = s"/address-management-api/address"
    val addressUnitId = 1234L
    val postcode = "L4 0TH"
    val simpleAddress = SimpleAddress(
      addressUnitId = Some(addressUnitId),
      line1 = "Liverpool FC, First team",
      line2 = "Anfield Stadium",
      line3 = "Anfield Road",
      line4 = "Liverpool",
      postcode = postcode
    )
    val detailedAddress = DetailedAddress(
      addressUnitId = Some(addressUnitId),
      nonAbpAddressId = Some(1234),
      organisationName = Some("Liverpool FC"),
      departmentName = Some("First team"),
      buildingName = Some("Anfield Stadium"),
      dependentThoroughfareName = Some("Anfield Road"),
      postTown = "Liverpool",
      postcode = postcode
    )
    val addressLookupResult = Addresses(Seq(detailedAddress))
  }

  "AddressConnector.get" should {
    "return the address associated with the address unit id" when {
      "it's returned from modernised" in new Setup {
        when(mockDefaultHttpClient.GET[Addresses](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(addressLookupResult))

        connector.get(addressUnitId).futureValue shouldBe Some(simpleAddress)
      }
    }
    "return None" when {
      "the call to modernised fails for whatever reason" in new Setup {
        when(mockDefaultHttpClient.GET[Addresses](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.failed(new RuntimeException()))

        connector.get(addressUnitId).futureValue shouldBe None
      }
    }
  }

  "find an address by postcode" should {
    "return a detailed address" when {
      "it's returned from modernised" in new Setup {
        when(mockDefaultHttpClient.GET[Addresses](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(addressLookupResult))

        connector.find(postcode).futureValue.loneElement shouldBe detailedAddress
      }
    }
  }

  "creating a non standard address" should {
    "return the ID of the newly created address" when {
      "the POST is successful and an ID is returned in the response body" in new Setup {
        val newAddressId = 123L
        when(mockDefaultHttpClient.POST[DetailedAddress, JsValue](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(Json.obj("id" -> newAddressId)))

        connector.create(simpleAddress).futureValue shouldBe newAddressId
      }
    }
    "fail with an exception" when {
      "the POST is successful but no ID can be extracted from the response body" in new Setup {
        when(mockDefaultHttpClient.POST[DetailedAddress, JsValue](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(Json.obj("foo" -> "bar")))

        whenReady(connector.create(simpleAddress).failed) { e =>
          e shouldBe an[Exception]
          e.getMessage shouldBe s"Failed to create record for address $simpleAddress"
        }
      }
    }
  }

}
