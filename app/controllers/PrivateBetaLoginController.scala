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

package controllers

import config.ApplicationConfig
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, Result}
import play.modules.reactivemongo.MongoDbConnection
import repositories.{LoginAttemptsMongoRepo, LoginAttemptsRepo}
import uk.gov.hmrc.play.http.HeaderNames._

import scala.concurrent.Future

trait PrivateBetaLoginController extends PropertyLinkingBaseController {

  val config: ApplicationConfig
  val repo: LoginAttemptsRepo
  lazy val privateBetaPassword = config.privateBetaPassword

  def login = Action.async(parse.json) { implicit request =>
    (for {
      pw <- (request.body \ "password").toOption
      ip <- request.headers.get(trueClientIp)
    } yield {
      verify(ip, pw.as[String])
    }).getOrElse(BadRequest)
  }

  private def verify(ip: String, password: String): Future[Result] = {
    repo.mostRecent(ip, config.maxAttempts, DateTime.now.minusHours(config.lockoutHours)) flatMap { failures =>
      (config.passwordValidationEnabled, failures.length, password) match {
        case (false, _, _) => Ok(Json.obj())
        case (_, l, _) if l >= config.maxAttempts => handleFailure(ip, Some("LOCKED_OUT"))
        case (_, _, `privateBetaPassword`) => Ok(Json.obj())
        case (_, _, p) => handleFailure(ip)
      }
    }
  }

  private def handleFailure(ipAddress: String, error: Option[String] = None): Future[Result] = repo.recordFailure(ipAddress) map { _ =>
    error match {
      case Some(err) => Unauthorized(Json.obj("errorCode" -> err))
      case _ => Unauthorized
    }
  }
}

object PrivateBetaLoginController extends PrivateBetaLoginController with MongoDbConnection {
  override val config = ApplicationConfig
  override val repo = new LoginAttemptsMongoRepo(db)
}
