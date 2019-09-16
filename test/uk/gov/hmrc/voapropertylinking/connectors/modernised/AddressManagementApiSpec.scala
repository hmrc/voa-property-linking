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

import basespecs.BaseUnitSpec
import models._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future

class AddressManagementApiSpec extends BaseUnitSpec {

  val testConnector = new AddressManagementApi(mockDefaultHttpClient, mock[ServicesConfig])

  val url = s"/address-management-api/address"

  "AddressConnector.get" should {
    "return the address associated with the address unit id" in {
      val addressUnitId = 1234L
      when(mockDefaultHttpClient.GET[APIAddressLookupResult](any())(any(), any(), any()))
        .thenReturn(Future.successful(APIAddressLookupResult(Seq(
          DetailedAddress(
            addressUnitId = Some(123456789),
            nonAbpAddressId = Some(1234),
            organisationName = Some("Liverpool FC"),
            departmentName = Some("First team"),
            buildingName = Some("Anfield Stadium"),
            dependentThoroughfareName = Some("Anfield Road"),
            postTown = "Liverpool",
            postcode = "L4 0TH"
          )))))

      testConnector.get(addressUnitId)(hc).futureValue shouldBe Some(SimpleAddress(
        addressUnitId = Some(123456789),
        line1 = "Liverpool FC, First team",
        line2 = "Anfield Stadium",
        line3 = "Anfield Road",
        line4 = "Liverpool",
        postcode = "L4 0TH")
      )
    }
  }
}
