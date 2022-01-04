/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.voapropertylinking.actions

import basespecs.BaseControllerSpec
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthenticatedActionBuilderSpec extends BaseControllerSpec {

  private trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    def success: ~[Option[String], Option[String]] = new ~(Some("gg_external_id_1234"), Some("gg_group_id_1234"))

    def exception: Option[AuthorisationException] = None

    val authConnector: AuthConnector = new AuthConnector {
      def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(
            implicit hc: HeaderCarrier,
            ec: ExecutionContext): Future[A] =
        exception.fold(Future.successful(success.asInstanceOf[A]))(Future.failed(_))
    }

    val authenticatedAction = new AuthenticatedActionBuilder(Helpers.stubControllerComponents(), authConnector)

  }

  "authorising the request" should {

    "execute the supplied body with the government gateway details when the External ID and Group ID exist" in new Setup {
      val action = authenticatedAction.async { request =>
        Future.successful(Ok(Json.toJson(request.principal)))
      }

      val result: Future[Result] = action(FakeRequest())

      val json: JsValue = contentAsJson(result)

      (json \ "externalId").as[String] shouldBe "gg_external_id_1234"
      (json \ "groupId").as[String] shouldBe "gg_group_id_1234"
    }

    "return an unauthorised exception if the authorisation fails due to a missing BearerToken" in new Setup {

      override def exception = Some(new MissingBearerToken)

      val action = authenticatedAction.async(_ => Future.successful(Ok("")))

      val result: Future[Result] = action(FakeRequest())

      whenReady(result.failed)(_ shouldBe a[MissingBearerToken])
    }

    "return an unauthorised exception if the user does not have either a group ID or a external ID" in new Setup {
      override def success = new ~(None, None)

      val action = authenticatedAction.async { request =>
        Future.successful(Ok(Json.toJson(request.principal)))
      }

      val result: Future[Result] = action(FakeRequest())
      val json: JsValue = contentAsJson(result)

      status(result) shouldBe UNAUTHORIZED
      (json \ "errorCode").as[String] shouldBe "NO_EXTERNAL_ID_OR_GROUP_ID"
    }

    "return an unauthorised exception if the user does not have a external ID" in new Setup {
      override def success = new ~(None, Some("gg_group_id_1234"))

      val action = authenticatedAction.async { request =>
        Future.successful(Ok(Json.toJson(request.principal)))
      }

      val result: Future[Result] = action(FakeRequest())
      val json: JsValue = contentAsJson(result)

      status(result) shouldBe UNAUTHORIZED
      (json \ "errorCode").as[String] shouldBe "NO_EXTERNAL_ID"
    }

    "return an unauthorised exception if the user does not have a group ID " in new Setup {
      override def success = new ~(Some("gg_external_id_1234"), None)

      val action = authenticatedAction.async { request =>
        Future.successful(Ok(Json.toJson(request.principal)))
      }

      val result: Future[Result] = action(FakeRequest())
      val json: JsValue = contentAsJson(result)

      status(result) shouldBe UNAUTHORIZED
      (json \ "errorCode").as[String] shouldBe "NO_GROUP_ID"
    }
  }

}
