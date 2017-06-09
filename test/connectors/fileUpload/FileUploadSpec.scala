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

import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.WireMockSpec
import helpers.WithSimpleWsHttpTestApplication
import infrastructure.SimpleWSHttp
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSClient
import uk.gov.hmrc.play.filters.MicroserviceFilterSupport
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.WithFakeApplication

class FileUploadSpec extends WireMockSpec with WithSimpleWsHttpTestApplication with MicroserviceFilterSupport {

  "FileUploadConnector" should {
    "be able to download files from the file upload service" in {
      implicit val fakeHc = HeaderCarrier()

      val fileBytes = getClass.getResource("/document.pdf").getFile.getBytes
      val fileUrl = s"/file-upload/envelopes/${java.util.UUID.randomUUID()}/files/document.pdf/content"
      val wsClient = fakeApplication.injector.instanceOf[WSClient]
      val http = fakeApplication.injector.instanceOf[SimpleWSHttp]
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

      await(await(connector.downloadFile(fileUrl)).body.runFold("")(_ + _.decodeString("UTF-8"))) should be(new String(fileBytes))
    }
  }
}
