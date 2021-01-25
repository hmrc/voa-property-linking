/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.voapropertylinking.repositories.SequenceGeneratorMongoRepository

import scala.concurrent.Future

class SubmissionIdControllerSpec extends BaseControllerSpec {

  trait Setup {
    val prefix = "pReFiX"
    val mockSequenceGeneratorMongoRepository: SequenceGeneratorMongoRepository =
      mock[SequenceGeneratorMongoRepository]
    when(mockSequenceGeneratorMongoRepository.getNextSequenceId(prefix)).thenReturn(Future.successful(100000L))
    val submissionIdController = new SubmissionIdController(
      Helpers.stubControllerComponents(),
      preAuthenticatedActionBuilders(),
      mockSequenceGeneratorMongoRepository)
  }

  "getting a submission id from the controller" should {
    "return a correctly formatted ID with specified prefix" in new Setup {
      Json
        .parse(
          contentAsString(submissionIdController.get(prefix)(FakeRequest()))
        )
        .as[String] shouldBe "PREFIX5HZ4"
    }
  }

  "formatting the submission ID" should {
    "uppercase the prefix and map characters avoiding the disallowed 's', 't', and 'u'" in new Setup {
      val submissionIds: List[String] =
        List.tabulate(1000)(n => submissionIdController.formatId(prefix, (n + 1000000).toLong))
      forAll(submissionIds) { (submissionId: String) =>
        submissionId should startWith(prefix.toUpperCase)
        submissionId.toList should contain noElementsOf Set('S', 'T', 'U')
      }
    }
  }

}
