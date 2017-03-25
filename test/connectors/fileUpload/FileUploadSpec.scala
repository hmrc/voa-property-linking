/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors.fileUpload

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockSpec
import infrastructure.WSHttp
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.HeaderCarrier

class FileUploadSpec extends WireMockSpec with MicroserviceFilterSupport {

  "FileUploadConnector" should {
    "be able to download files from the file upload service" in {
      implicit val fakeHc = HeaderCarrier()

      val fileBytes = getClass.getResource("/document.pdf").getFile.getBytes
      val fileUrl = s"/file-upload/envelopes/${java.util.UUID.randomUUID()}/files/document.pdf/content"
      val wsClient = app.injector.instanceOf[WSClient]
      val http = app.injector.instanceOf[WSHttp]
      val connector = new FileUploadConnector(wsClient, http) {
        override lazy val url = mockServerUrl
      }

      stubFor(get(urlEqualTo(fileUrl))
        .willReturn(aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/octet-stream")
          .withBody(fileBytes)
        )
      )

      await(connector.downloadFile(fileUrl)) should be(fileBytes)
    }
  }

}
