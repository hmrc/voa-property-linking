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

//package connectors
//
//import auth.AuthenticatedApiRequest
//import http.VoaHttpClient
//import javax.inject.{Inject, Named}
//import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
//import auth.{AuthenticatedApiRequest, Principal}
//import models.ModernisedEnrichedRequest
//
//import scala.concurrent.{ExecutionContext, Future}
//
//class PropertyLinkSubmissionIdConnector @Inject()(
//                                                   val http: VoaHttpClient,
//                                                   @Named("propertyLinking.submissionIdUrl") submissionIdUrl: String)(implicit executionContext: ExecutionContext)
//  {
//
//  //override protected def sourceApi: SourceApi = PropertyLinkingApi
//
////    implicit def hc(implicit request: AuthenticatedApiRequest[_]): HeaderCarrier =
////      request.headerCarrier
////
////    implicit def principal(implicit request: AuthenticatedApiRequest[_]): Principal =
////      request.principal
//
////    implicit def principal(implicit request: AuthenticatedApiRequest[_]): ModernisedEnrichedRequest =
////      request.principal
//
//  def get(prefix: String)(implicit hc: HeaderCarrier, request: ModernisedEnrichedRequest[_])
//  : Future[String] =
//    http.GET[String](s"$submissionIdUrl/$prefix")
//}