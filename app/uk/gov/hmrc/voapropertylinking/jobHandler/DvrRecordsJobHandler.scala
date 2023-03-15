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

package uk.gov.hmrc.voapropertylinking.jobHandler

import play.api.{Logger, LoggerLike}
import uk.gov.hmrc.voapropertylinking.repositories.DVRRepository
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DvrRecordsJobHandler @Inject()(dvrRepository: DVRRepository)(implicit ec: ExecutionContext) {

  val log: LoggerLike = Logger(this.getClass)

  def processJob(): Future[Unit] =
    for {
      findDocumentsNoTimestamp  <- dvrRepository.findDocumentsNoTimestamp
      updatedCreatedAtTimestamp <- {
        log.info(s"ids found $findDocumentsNoTimestamp")
        dvrRepository.updateCreatedAtTimestampById(findDocumentsNoTimestamp)
      }
    } yield {
      log.info(s"updated $updatedCreatedAtTimestamp")
      if (updatedCreatedAtTimestamp > 0) {
        log.info(s"Successful updated: $updatedCreatedAtTimestamp createdAt Strings to use current LocalDateTime")
      } else {
        log.info("No dvr records updated")
      }
    }
}
