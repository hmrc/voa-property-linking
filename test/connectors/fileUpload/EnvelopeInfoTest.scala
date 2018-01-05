/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatest.{FlatSpec, MustMatchers}
import play.api.libs.json.Json

class EnvelopeInfoTest extends FlatSpec with MustMatchers {

  "EnvelopeInfo" should "be deserialised property" in {
    val envelope = Json.parse(DummyData.envelopeContent).as[EnvelopeInfo]
    envelope.status mustBe "OPEN"
    envelope.files.map(_.id) mustBe Seq("index.jpeg", "index2.jpeg")
  }

  "EnvelopeInfo.files" should "be optional" in {
    val envelope = Json.parse(DummyData.envelopeWithNoFiles).as[EnvelopeInfo]
    envelope.status mustBe "OPEN"
    envelope.files mustBe Nil
  }
}

  object DummyData {
    val envelopeContent =
      """{
        |  "id": "8d227d24-4330-49b1-b405-145196a975b9",
        |  "status": "OPEN",
        |  "destination": "VOA_CCA",
        |  "application": "application/json",
        |  "files": [
        |    {
        |      "id": "index.jpeg",
        |      "status": "QUARANTINED",
        |      "name": "index.jpeg",
        |      "contentType": "image/jpeg",
        |      "created": "2016-11-09T15:50:59Z",
        |      "metadata": {},
        |      "href": "/file-upload/envelopes/8d227d24-4330-49b1-b405-145196a975b9/files/index.jpeg/content"
        |    },
        |    {
        |      "id": "index2.jpeg",
        |      "status": "QUARANTINED",
        |      "name": "index2.jpeg",
        |      "contentType": "image/jpeg",
        |      "created": "2016-11-09T15:50:59Z",
        |      "metadata": {},
        |      "href": "/file-upload/envelopes/8d227d24-4330-49b1-b405-145196a975b9/files/index2.jpeg/content"
        |    }
        |  ],
        |  "metadata": {
        |     "submissionId": "aSubmissionId",
        |     "personId": 12345
        |  }
        |}
      """.stripMargin
    val envelopeWithNoFiles =
      """{
        |  "id": "8d227d24-4330-49b1-b405-145196a975b9",
        |  "status": "OPEN",
        |  "destination": "VOA_CCA",
        |  "application": "application/json",
        |  "metadata": {
        |     "submissionId": "aSubmissionId",
        |     "personId": 12345
        |  }
        |}
      """.stripMargin
    val envelopesData = """
        | {
        |  "_links": {
        |    "self": {
        |      "href": "http://full.url.com/file-transfer/envelopes?destination=DMS"
        |    }
        |  },
        |  "_embedded": {
        |    "envelopes": [
        |      {
        |        "id": "0b215e97-11d4-4006-91db-c067e74fc653",
        |        "destination": "DMS",
        |        "application": "application:digital.forms.service/v1.233",
        |        "_embedded": {
        |          "files": [
        |            {
        |              "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/1/content",
        |              "name": "original-file-name-on-disk.docx",
        |              "contentType": "application/vnd.oasis.opendocument.spreadsheet",
        |              "length": 1231222,
        |              "created": "2016-03-31T12:33:45Z",
        |              "_links": {
        |                "self": {
        |                  "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/1"
        |                }
        |              }
        |            },
        |            {
        |              "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/2/content",
        |              "name": "another-file-name-on-disk.docx",
        |              "contentType": "application/vnd.oasis.opendocument.spreadsheet",
        |              "length": 112221,
        |              "created": "2016-03-31T12:33:45Z",
        |              "_links": {
        |                "self": {
        |                  "href": "/file-upload/envelopes/0b215e97-11d4-4006-91db-c067e74fc653/files/2"
        |                }
        |              }
        |            }
        |          ]
        |        },
        |        "_links": {
        |          "self": {
        |            "href": "/file-transfer/envelopes/0b215e97-11d4-4006-91db-c067e74fc653"
        |          },
        |          "package": {
        |            "href": "/file-transfer/envelopes/0b215e97-11d4-4006-91db-c067e74fc653",
        |            "type": "application/zip"
        |          },
        |          "files": [
        |            {
        |              "href": "/files/2"
        |            }
        |          ]
        |        }
        |      }
        |    ]
        |  }
        |}""".stripMargin
}
