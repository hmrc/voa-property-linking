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

import akka.stream.scaladsl.Source
import com.codahale.metrics.MetricRegistry
import com.google.inject.Singleton
import com.kenshoo.play.metrics.Metrics
import connectors.EvidenceConnector
import connectors.fileUpload.{EnvelopeInfo, EnvelopeMetadata, FileInfo, FileUploadConnector}
import metrics.MetricsLogger
import models.{Closed, Open}
import play.api.Logger
import repositories.EnvelopeIdRepo
import uk.gov.hmrc.play.http.{HeaderCarrier, Upstream4xxResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.NonFatal

case class FUAASDownloadException(href: String, status: Int) extends Exception(s"Failed to download $href (status: $status)")

@Singleton
class FileTransferService @Inject()(val fileUploadConnector: FileUploadConnector,
                                    val evidenceConnector: EvidenceConnector,
                                    val repo: EnvelopeIdRepo,
                                    override val metrics: Metrics) extends MetricsLogger {
  implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext

  def transferManually(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    repo.getStatus(envelopeId) flatMap {
      case Some(Closed) => transferSingleFile(envelopeId)
      case _ => Logger.info(s"Received callback for $envelopeId, but declaration has not been submitted"); Future.successful(())
    }
  }

  private def transferSingleFile(envelopeId: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    {
      for {
        info <- fileUploadConnector.getEnvelopeDetails(envelopeId)
        _ <- processEnvelope(info)
      } yield ()
    } recover {
      case NonFatal(t) => Logger.warn(s"Transfer of $envelopeId failed due to ${t.getMessage}")
    }
  }

  def justDoIt()(implicit hc: HeaderCarrier): Future[FileTransferComplete] = {
    val allEnvelopes = repo.get()
    allEnvelopes.foreach { envelopes =>
      val (open, closed) = envelopes.span(_.status.contains(Open))
      logMetrics("mongo.envelope.queue-size", "open", open.size)
      logMetrics("mongo.envelope.queue-size", "closed", closed.size)
      Logger.info(s"${open.size} open, ${closed.size} closed")
      Logger.info(s"${envelopes.size} envelopes found in mongo: ${envelopes.map(x => (x.envelopeId, x.status))}")
    }

    {
      for {
        closedEnvelopes <- allEnvelopes.map(_.filter(_.status.getOrElse(Closed) == Closed))
        envelopeIds = closedEnvelopes.map(_.envelopeId)
        envelopeInfos <- Future.traverse(envelopeIds)( envId => fileUploadConnector.getEnvelopeDetails(envId))
        envelopeFilesNotQuarantine = envelopeInfos.filterNot(env => env.files.map(_.status).contains("QUARANTINED"))
        _ <- envelopeFilesNotQuarantine.foldLeft(Future.successful(())) {
          case (f, envInfo) => f.flatMap(_ => processEnvelope(envInfo)).recover {
            case ex: FUAASDownloadException =>
              Logger.warn(s"Skipping FUaaS download ${ex.href} as it returned ${ex.status}; continuing processing next envelope")
          }
        }
      } yield {
        Logger.info("Ending transfer job run")
        FileTransferComplete(None)
      }
    }.recoverWith {
      case e: Upstream4xxResponse if e.upstreamResponseCode == 429 =>
        Logger.warn("Rate limit exceeded, terminating queue processing")
        Future.successful(FileTransferComplete(None))
    }
  }

  private def removeEnvelopes(envInfo: EnvelopeInfo)(implicit hc: HeaderCarrier): Future[Unit] = {
    envInfo.status match {
      case "NOT_EXISTING" => Future.successful(repo.remove(envInfo.id))
      case "UNKNOWN_ERROR" => moveToBackOfQueue(envInfo.id)(new Exception("Error when retrieving envelope data"))
      case _ => fileUploadConnector.deleteEnvelope(envInfo.id).flatMap(_ => repo.remove(envInfo.id))
    }
  }

  private[services] def transferFile(fileInfo: FileInfo, metadata: EnvelopeMetadata)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      file <- fileUploadConnector.downloadFile(fileInfo.href)
      r <- if(file.headers.status < 400)
            evidenceConnector.uploadFile(fileInfo.name, file.body, metadata)
           else
            failedDownloadFromFUAAS(fileInfo, file.headers.status)
    } yield r
  }

  private def failedDownloadFromFUAAS(fileInfo: FileInfo, status: Int): Future[Unit] = {
    Future.failed(FUAASDownloadException(fileInfo.href, status))
  }

  private def processEnvelope(envelopeInfo: EnvelopeInfo)(implicit hc: HeaderCarrier): Future[Unit] = {
    val logMsg = s"Processing ${envelopeInfo.files.size} files: from envelope: ${envelopeInfo.id}"

    envelopeInfo.files.foldLeft(Future(Logger.info(logMsg))) { (prev, fileInfo) =>
      prev.flatMap { _ =>
        fileInfo.status match {
          case "ERROR" => evidenceConnector.uploadFile(fileInfo.name, Source.empty, envelopeInfo.metadata)
          case _ => transferFile(fileInfo, envelopeInfo.metadata)
        }
      }
    } recoverWith moveToBackOfQueue(envelopeInfo.id) flatMap { _ => removeEnvelopes(envelopeInfo) }
  }

  private def moveToBackOfQueue(envelopeId: String): PartialFunction[Throwable, Future[Unit]] = {
    case e: FUAASDownloadException => Future.failed(e)
    case e: Upstream4xxResponse if e.upstreamResponseCode == 429 =>
      Logger.warn(s"Rate limit exceeded - will resume at envelope $envelopeId next run")
      logMetrics("modernized.upload", "rate-limit")
      Future.failed(e)
    case e: Throwable =>
      Logger.error(s"Error processing file(s) in envelope $envelopeId to backend - moving to back of queue", e)
      repo.remove(envelopeId).flatMap(_ => repo.create(envelopeId, Closed)).flatMap(_ => Future.failed(e))
  }
}
