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

package connectors

import binders.GetPropertyLinksParameters
import http.VoaHttpClient
import models.modernised._
import models.{ModernisedEnrichedRequest, PaginationParams}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.config.ServicesConfig
//import org.scalatest.mock.MockitoSugar
//import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import util.Cats
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.mockito.Mockito._


import scala.concurrent.{ExecutionContext, Future}

class ExternalPropertyLinkConnectorSpec extends UnitSpec with MockitoSugar with WithFakeApplication with BeforeAndAfterEach with BeforeAndAfterAll with Matchers with Inspectors with Inside
  with EitherValues with LoneElement with ScalaFutures with OptionValues with Cats {


    trait Setup {
      implicit val modernisedEnrichedRequest = ModernisedEnrichedRequest(FakeRequest(), "XXXXX", "YYYYY")
      implicit val ec: ExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
      implicit val fakeHc = HeaderCarrier()


      val address = "mock address"
      val baref = "mock baref"
      val agent = "mock agent"
      val status = "mock status"
      val sortField = "mock sort field"
      val sortOrder = "mock sort order"
      val searchParams = GetPropertyLinksParameters(address = Some(address), baref = Some(baref), agent = Some(agent), status = Some(status),
        Some(sortField), Some(sortOrder))

      val emptySearchParams = GetPropertyLinksParameters()

      val voaApiUrl = "http://voa-modernised-api/external-property-link-management-api"

      val ownerAuthorisationUrl = s"$voaApiUrl/my-organisation/property-links/{propertyLinkId}"
      val ownerAuthorisationsUrl = s"$voaApiUrl/my-organisation/property-links"
      val clientAuthorisationUrl = s"$voaApiUrl/my-organisation/clients/all/property-links/{propertyLinkId}"
      val clientAuthorisationsUrl = s"$voaApiUrl/my-organisation/clients/all/property-links"
      val createPropertyLinkUrl = s"$voaApiUrl/my-organisation/property-links"
      val httpstring = "VoaAuthedBackendHttp"

      val connector = new ExternalPropertyLinkConnector(
        http = mock[VoaHttpClient],
        myOrganisationsPropertyLinksUrl = ownerAuthorisationsUrl,
        myOrganisationsPropertyLinkUrl = ownerAuthorisationUrl,
        myClientsPropertyLinkUrl = clientAuthorisationUrl,
        myClientsPropertyLinksUrl = clientAuthorisationsUrl,
        createPropertyLinkUrl = createPropertyLinkUrl,
        conf = fakeApplication.injector.instanceOf[ServicesConfig]
        //voaClientExceptionMapper = new VoaClientExceptionMapper(List.empty)
      )

      val paginationParams = PaginationParams(1, 1, true)
      val queryParams: Seq[(String, String)] = Seq(
        ("start", paginationParams.startPoint.toString),
        ("size", paginationParams.pageSize.toString),
        ("requestTotalRowCount", "true"))
    }

    "get my organisations property links" should {

      "build the correct query params and call the modernised layer" in new Setup {

        val mockReturnedPropertyLinks: PropertyLinksWithAgents = mock[PropertyLinksWithAgents]

        when(connector.http.GET[PropertyLinksWithAgents](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(mockReturnedPropertyLinks))

        connector
          .getMyOrganisationsPropertyLinks(emptySearchParams, params = Some(paginationParams))
          .futureValue shouldBe mockReturnedPropertyLinks

        verify(connector.http)
          .GET(mEq(ownerAuthorisationsUrl), mEq(queryParams))(any(), any(), any(), any())
      }

    }

    "get my organisations single property link with submissionId" should {

      "build the correct url and calls the modernised layer" in new Setup {

        val mockReturnedPropertyLink: PropertyLinkWithAgents = mock[PropertyLinkWithAgents]

        when(connector.http.GET[Option[PropertyLinkWithAgents]](any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(Some(mockReturnedPropertyLink)))

        connector.getMyOrganisationsPropertyLink("PL1").futureValue shouldBe Some(mockReturnedPropertyLink)

        verify(connector.http)
          .GET(mEq(ownerAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
      }
    }

    "get clients property links" should {

      "build the correct query params and call the modernised layer" in new Setup {

        val mockReturnedPropertyLinks: PropertyLinksWithClient = mock[PropertyLinksWithClient]

        when(connector.http.GET[PropertyLinksWithClient](any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(mockReturnedPropertyLinks))

        connector.getClientsPropertyLinks(searchParams, Some(paginationParams)).futureValue shouldBe mockReturnedPropertyLinks

        val clientQueryParams = queryParams :+ ("address" -> address) :+ ("baref" -> baref):+ ("agent" -> agent):+ ("status" -> status):+ ("sortfield" -> sortField):+ ("sortorder" -> sortOrder)
        verify(connector.http).GET(mEq(clientAuthorisationsUrl), mEq(clientQueryParams))(any(), any(), any(), any())
      }

    }

    "get clients single property link with submissionId" should {

      "build the correct url and calls the modernised layer" in new Setup {

        val mockReturnedPropertyLink: ClientPropertyLink = mock[ClientPropertyLink]

        when(connector.http.GET[ClientPropertyLink](any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(mockReturnedPropertyLink))

        connector.getClientsPropertyLink("PL1").futureValue shouldBe mockReturnedPropertyLink

        verify(connector.http)
          .GET(mEq(clientAuthorisationUrl.replace("{propertyLinkId}", "PL1")))(any(), any(), any(), any())
      }
    }

    "create property link" should {

      "call modernised createPropertyLink endpoint" in new Setup {

        //val mockSubmissionId: CreatePropertyLinkResponse = mock[CreatePropertyLinkResponse]
        val mockHttpResponse: HttpResponse = mock[HttpResponse]
        val mockVoaCreatePropertyLink: CreatePropertyLink = mock[CreatePropertyLink]

        when(mockVoaCreatePropertyLink.PLsubmissionId).thenReturn("PL123")

        when(
          connector.http
            .POST[CreatePropertyLink, HttpResponse](any(), any(), any())(any(), any(), any(), any(), any()))
          .thenReturn(Future.successful(mockHttpResponse))

        connector.createPropertyLink(mockVoaCreatePropertyLink).futureValue shouldBe mockHttpResponse

        verify(connector.http)
          .POST(mEq(createPropertyLinkUrl), mEq(mockVoaCreatePropertyLink), mEq(Seq()))(
            any(),
            any(),
            any(),
            any(),
            any())
      }

    }

}
