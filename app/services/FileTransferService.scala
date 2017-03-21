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

package services

import javax.inject.Inject

import connectors.EvidenceConnector
import connectors.fileUpload.{EnvelopeInfo, EnvelopeMetadata, FileInfo, FileUploadConnector}
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FileTransferService @Inject()(val fileUploadConnector: FileUploadConnector,
                                    val evidenceConnector: EvidenceConnector,
                                    val repo: EnvelopeIdRepo
                                   )
  extends MongoDbConnection {

  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def justDoIt()(implicit hc: HeaderCarrier): Future[Unit] = {
    val envelopeIds = repo.get()
    envelopeIds.foreach(envId => Logger.info(s"Envelope Id: $envId found in mongo"))

    val envelopeAndFiles = envelopeIds.map(_.map(envId => fileUploadConnector.getEnvelopeDetails(envId)))
      .flatMap(x => Future.sequence(x))

    envelopeAndFiles.flatMap { es => Future.sequence {
        es.map(envInfo => envInfo.status match {
          case "CLOSED" if envInfo.files.isEmpty => Future.successful(removeEnvelopes(envInfo))
          case "CLOSED" => processClosedNotEmptyEnvelope(envInfo)
          case "NOT_EXISTING" => Future.successful(repo.remove(envInfo.id))
          case "OPEN" => Future.successful(())
          case _ if !envInfo.files.map(_.status).contains("QUARANTINED") => processNotYetClosedEnvelopes(envInfo)
          case _ => Future.successful(()) //Some files haven't been virus checked yet.
        })
      }
    } map { _ => () }
  }

  private def removeEnvelopes(envInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
//    fileUploadConnector.deleteEnvelope(envInfo.id).map(_ => repo.remove(envInfo.id)) TODO work out which endpoint to call
    repo.remove(envInfo.id)
  }

  private def processClosedNotEmptyEnvelope(envelopeInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
    val fileInfos = envelopeInfo.files
    Future.sequence(fileInfos.map(fileInfo => {
      transferFile(fileInfo, envelopeInfo.metadata)
    }))
      .map(_ => removeEnvelopes(envelopeInfo))
  }

  def transferFile(fileInfo: FileInfo, metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      file <- fileUploadConnector.downloadFile(fileInfo.href)
      _ = Logger.info(s"Downloaded file ${fileInfo.name}")
      _ <- evidenceConnector.uploadFile(fileInfo.name, if (file.isEmpty) None else Some(file), metadata)
    } yield ()
  }

  private def processNotYetClosedEnvelopes(envelopeInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
    val envId = envelopeInfo.id
    Future.sequence(envelopeInfo.files.map(fileInfo => {
      fileInfo.status match {
        case "ERROR" =>
          evidenceConnector.uploadFile(fileInfo.name, None, envelopeInfo.metadata)
        case _ => transferFile(fileInfo, envelopeInfo.metadata)
      }
    })).map(_ => removeEnvelopes(envelopeInfo))
  }
}
