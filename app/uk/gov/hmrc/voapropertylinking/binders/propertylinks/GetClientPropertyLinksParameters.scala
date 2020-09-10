/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.voapropertylinking.binders.propertylinks

import java.time.LocalDate

import binders._
import uk.gov.hmrc.voapropertylinking.binders.validation.ValidatingBinder
case class GetClientPropertyLinksParameters(
      address: Option[String] = None,
      baref: Option[String] = None,
      uarn: Option[Long] = None,
      status: Option[String] = None,
      representationStatus: Option[String] = None,
      client: Option[String] = None,
      sortfield: Option[String] = None,
      sortOrder: Option[String] = None,
      appointedFromDate: Option[LocalDate] = None,
      appointedToDate: Option[LocalDate] = None

)

object GetClientPropertyLinksParameters extends ValidatingBinder[GetClientPropertyLinksParameters] {

  override def validate(params: Params): ValidationResult[GetClientPropertyLinksParameters] =
    (
      validateString("address", params),
      validateString("baref", params),
      validateString("uarn", params),
      validateString("status", params),
      validateString("representationStatus", params),
      validateString("sortfield", params),
      validateString("sortorder", params),
      validateLocalDate("appointedFromDate", params),
      validateLocalDate("appointedToDate", params)).mapN(GetClientPropertyLinksParameters.apply)

  def validateString(implicit key: String, params: Params): ValidationResult[Option[String]] =
    readOption(key, params)

  def validateLocalDate(implicit key: String, params: Params): ValidationResult[Option[LocalDate]] =
    readOption ifPresent asLocalDate

}
