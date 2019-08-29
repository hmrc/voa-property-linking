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

package uk.gov.hmrc.voapropertylinking.utils

import play.api.Logger
import uk.gov.hmrc.voapropertylinking.auth.Principal

import scala.collection.SortedMap
import scala.util.Try

sealed abstract class LogEvent(val code: String, val description: String) {
  override def toString: String = s"#BRA$code - $description"
}

trait EventLogging {

  private lazy val logger: Logger = Logger("eventLoggerVPL")

  protected def logResponse(logEvent: LogEvent, data: (String, Any)*)(implicit principal: Principal): Unit =
    Try {
      val pairs = data.toList :+ ("grpId" -> principal.groupId) :+ ("extId" -> principal.externalId)
      val dataString = SortedMap[String, Any](pairs: _*)
        .collect {
          case (k, v) => s"$k = ${v.toString}"
        }
        .mkString("[ ", " , ", " ]")
      logger.info(s"${logEvent.toString} - $dataString")
    }

  /* c0xy - GENERIC EVENTS */
  case object PropertySearchResultsReturned extends LogEvent("2001", "Property search results returned")
  case object InternalServerErrorEvent extends LogEvent("5000", "Unexpected exception occurred")
  case object VoaErrorOccurred extends LogEvent("5001", "Error response received from the modernised tier")

  /* c1xy - VALUATION RELATED EVENTS */
  case object SummaryValuationReturned extends LogEvent("2101", "Summary valuation returned")
  case object ValuationHistoryReturned extends LogEvent("2102", "Valuation history returned")
  case object DetailedValuationReturned extends LogEvent("2103", "Detailed valuation returned")

  case object UnsupportedValuationProjection extends LogEvent("4101", "Unsupported valuation projection requested")

  /* c2xy - PROPERTY LINK RELATED EVENTS */
  case object PropertyLinkCreated extends LogEvent("2200", "Property link created")
  case object PropertyLinkCreationFailed extends LogEvent("5200", "Property link creation failed")
  case object MyOrganisationPropertyLinkReturned extends LogEvent("2201a", "My organisation property link returned")
  case object MyOrganisationPropertyLinksReturned extends LogEvent("2202a", "My organisation property links returned")
  case object MyClientsPropertyLinkReturned extends LogEvent("2201b", "My client's property link returned")
  case object MyClientsPropertyLinksReturned extends LogEvent("2202b", "My client's property links returned")

  /* c3xy - CHECK RELATED EVENTS */
  case object CheckCaseCreated extends LogEvent("2300", "Check case created")
  case object CheckCaseCreationFailed extends LogEvent("5300", "Check case creation failed")
  case object MyOrganisationCheckCaseReturned extends LogEvent("2301a", "My organisation check case returned")
  case object MyOrganisationCheckCasesReturned extends LogEvent("2302a", "My organisation check cases returned")
  case object MyClientsCheckCaseReturned extends LogEvent("2301b", "My client's check case returned")
  case object MyClientsCheckCasesReturned extends LogEvent("2302b", "My client's check cases returned")

  /* c4xy - CHALLENGE RELATED EVENTS */
  case object ChallengeCaseCreated extends LogEvent("2400", "Challenge case created")
  case object ChallengeCaseCreationFailed extends LogEvent("5400", "Challenge case creation failed")

  /* c6xy - LOOKUPS RELATED EVENTS */
  case object LookupsRetrieved extends LogEvent("2601", "Lookups retrieved")

  /* c8xy - ATTACHMENT RELATED EVENTS */
  case object PropertyLinkEvidenceUploadInitiated extends LogEvent("2820", "Property link evidence upload initiated")
  case object PropertyLinkEvidenceReturned extends LogEvent("2821", "Property link evidence upload returned")
  case object CheckCaseEvidenceUploadInitiated extends LogEvent("2830", "Check case evidence upload initiated")
  case object CheckCaseEvidenceReturned extends LogEvent("2831", "Check case evidence upload returned")
  case object ChallengeCaseUploadInitiated extends LogEvent("2840", "Challenge evidence upload initiated")
  case object ChallengeCaseEvidenceReturned extends LogEvent("2841", "Challenge case evidence upload returned")

  case object AttachmentsErrorOccurred extends LogEvent("5801", "Error response received from attachments service")

  /* c9xy - MESSAGES RELATED EVENTS */
  case object MessageSummaryReturned extends LogEvent("2901", "Message summary returned")
  case object DetailedMessageReturned extends LogEvent("2902", "Detailed message returned")
  case object UnreadMessageCountReturned extends LogEvent("2909", "Unread message count returned")
  case object MessageMarkedAsRead extends LogEvent("2903", "Mark message as read")

}
