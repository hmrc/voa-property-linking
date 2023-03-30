package uk.gov.hmrc.voapropertylinking.stubs.bst

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.voapropertylinking.{WiremockHelper, WiremockMethods}

trait CCACaseManagementStub extends WiremockMethods with WiremockHelper {


  def stubRequestDetailedValuation(request: DetailedValuationRequest)(response: HttpResponse): StubMapping =
    when(
      method = POST,
      uri = s"/cca-case-management-api/cca_case/dvrSubmission",
      body = request
    ).thenReturn(response.status, response.json)

}
