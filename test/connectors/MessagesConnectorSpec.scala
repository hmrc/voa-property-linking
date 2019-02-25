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

package connectors

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.SimpleWsHttpTestApplication
import models._
import models.messages._
import play.api.http.ContentTypes
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.ws.WSHttp

sealed trait MessageSortField

class MessagesConnectorSpec extends ContentTypes with WireMockSpec with SimpleWsHttpTestApplication {

  implicit val hc = HeaderCarrier()
  val http = fakeApplication.injector.instanceOf[WSHttp]
  val testConnector = new MessagesConnector(http, fakeApplication.injector.instanceOf[ServicesConfig]) {
    override lazy val baseUrl = mockServerUrl
  }

  val url = "/message-search-api"

  "MessagesConnectorSpec.getMessage" should {
    "return the message search results for the  given organisation id and message id" in {
      val orgId = 1234L
      val msgId = "abc1"
      val getMessageUrl = s"$url/messages?recipientOrganisationID=$orgId&objectID=$msgId"

      val stub = stubFor(get(urlEqualTo(getMessageUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(messageSearchResultsResponseValid)))

      val result = (await(testConnector.getMessage(recipientOrgId = orgId, messageId = msgId)(hc)))
      result shouldBe expectedMessageSearchResultsResponse
    }
  }

  "MessagesConnectorSpec.getMessageCount" should {
    "return the message count for the given org id" in {

      val orgId = 12345
      val messageCount = MessageCount(0, 10)
      val getCountUrl = s"$url/count/$orgId"

      val stub = stubFor(get(urlEqualTo(getCountUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(validMessageCount)))

      val result = (await(testConnector.getMessageCount(orgId)(hc)))
      result shouldBe messageCount
    }
  }


  "MessagesConnectorSpec.readMessage" should {
    "return the message unit indicating the message has been read" in {

      val messageId = "xyz123"
      val readBy  = "TheDonald"
      val now = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
      val getPatchUrl = s"$url/messages/$messageId?lastReadBy=$readBy&lastReadAt=$now"

      val stub = stubFor(patch(urlEqualTo(getPatchUrl))
        .willReturn(aResponse
          .withStatus(200)
          .withHeader("Content-Type", JSON)
          .withBody(getEmptyResponse)))

      val result = (await(testConnector.readMessage(messageId, readBy)(hc)))
      result shouldBe ()
    }
  }

  lazy val expectedMessageSearchResultsResponse =  MessageSearchResults(
    start = 0,
    size = 10,
    messages = Seq(
      Message(
        id = "id123",
        recipientOrgId = 987654,
        templateName = "template1",
        clientOrgId = Some(45678),
        clientName = Some("Donald Trump Snr"),
        agentOrgId = Some(12345),
        agentName = Some("agent1"),
        caseReference = "case1",
        submissionId = "submission1",
        timestamp = localDateTime,
        address = "Thee White House",
        effectiveDate = localDateTime,
        subject = "I m part of the resistance",
        lastRead = None,
        messageType = "Information"
      )
    )
  )

  lazy val individualAccountSubmission = IndividualAccountSubmission(
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

  lazy val expectedGetEmptyResponse = None

  lazy val getEmptyResponse = "{}"

  lazy val validMessageCount = """{
      "messageCount":0,
      "totalMessagesCount":10
   }""".stripMargin


  lazy val messageSearchResultsResponseValid = """{
      "start": 0,
      "size": 10,
      "messages": [
        {
          "objectID": "id123",
          "recipientOrganisationID": 987654,
          "templateName": "template1",
          "clientOrganisationID": 45678,
          "clientOrganisationName": "Donald Trump Snr",
          "agentOrganisationID": 12345,
          "agentOrganisationName": "agent1",
          "businessKey1": "case1",
          "businessKey2": "submission1",
          "dateTimeStamp": "2018-09-06T00:00:00",
          "address": "Thee White House",
          "effectiveDate": "2018-09-06T00:00",
          "subject": "I m part of the resistance",
          "messageType": "Information"
        }
      ]
    }""".stripMargin

  lazy val localDateTime = LocalDateTime.of(2018, 9, 6, 0, 0, 0)
}
