/*
 * Copyright 2016 HM Revenue & Customs
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
import connectors.fileUpload.{EnvelopeInfo, FileUploadConnector}
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import repositories.EnvelopeIdRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class FileTransferService @Inject() (val fileUploadConnector: FileUploadConnector,
                                     val evidenceConnector: EvidenceConnector,
                                     val repo: EnvelopeIdRepository
                                    )
  extends MongoDbConnection{

  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def justDoIt()(implicit hc: HeaderCarrier) = {
    val envelopeIds = repo.get()
    envelopeIds.foreach(envId => Logger.info(s"Envelope Id: $envId found in mongo"))

    val envelopeAndFiles = envelopeIds.map(_.map(envId => fileUploadConnector.getEnvelopeDetails(envId)))
      .map(x=> Future.sequence(x)).flatMap(identity)

    envelopeAndFiles.map(envInfos => {
      val closed = envInfos.filter(_.status == "CLOSED")
      val onlyInMongo = envInfos.filter(_.status == "NOT_EXISTING")
      val other = envInfos.filterNot(_.status == "CLOSED").filterNot(_.status=="NOT_EXISTING")
      envInfos.foreach ( envInfo => envInfo.status match {
        case "CLOSED" if envInfo.files.isEmpty => removeEnvelopes(envInfo)
        case "CLOSED" => processClosedNotEmptyEnvelope(envInfo)
        case "NOT_EXISTING" => repo.remove(envInfo.id)
        case _ if !envInfo.files.map(_.status).contains("QUARANTINED") => processNotYetClosedEnvelopes(envInfo)
        case _ => Future.successful(()) //Some files haven't been virus checked yet.
      })
    })
  }

  private def removeEnvelopes(envInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
    fileUploadConnector.deleteEnvelope(envInfo.id).map(_ => repo.remove(envInfo.id))
  }

  private def processClosedNotEmptyEnvelope(envelopeInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
    val fileUrls = envelopeInfo.files.map(_.href)
    Future.sequence(fileUrls.map( url => {
      transferFile(url)
    }))
      .map(_ => removeEnvelopes(envelopeInfo))
  }

  def transferFile(url: String)(implicit hc:HeaderCarrier): Future[Unit] = {
    fileUploadConnector.downloadFile(url).flatMap(_ => evidenceConnector.uploadFile)
  }

  private def processNotYetClosedEnvelopes(envelopeInfo: EnvelopeInfo)(implicit hc: HeaderCarrier) = {
    val envId = envelopeInfo.id
    Future.sequence(envelopeInfo.files.map(fileInfo =>{
      fileInfo.status match {
        case "ERROR" =>
          evidenceConnector.uploadFile //TODO - indicate error - should be a param to uploadFile.
        case _ => transferFile(fileInfo.href)
      }
    })).map(_=> fileUploadConnector.deleteEnvelope(envId)).map(_ => repo.remove(envId))
  }
}

