package uk.gov.hmrc.voapropertylinking.repositories

import models.modernised.ccacasemanagement.requests.DetailedValuationRequest
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{LoneElement, OptionValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.mongo.test.{CleanMongoCollectionSupport, PlayMongoRepositorySupport}
import uk.gov.hmrc.voapropertylinking.services.DateTimeService

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext

class DVRRepositorySpec
    extends AnyWordSpec with GuiceOneAppPerSuite with LoneElement with Matchers with OptionValues with ScalaFutures
    with PlayMongoRepositorySupport[DVRRecord] with CleanMongoCollectionSupport {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  val dateTimeService: DateTimeService = app.injector.instanceOf[DateTimeService]
  override val repository = new DVRRepository(mongoComponent, dateTimeService, "test-dvrRecords")

  val dvrRequest: DetailedValuationRequest = DetailedValuationRequest(
    authorisationId = 12L,
    organisationId = 34L,
    personId = 56L,
    submissionId = "DVR-123456",
    assessmentRef = 78L,
    agents = Some(List(90L)),
    billingAuthorityReferenceNumber = "01234"
  )
  val timestamp: LocalDateTime = LocalDateTime.of(2022, 12, 10, 1, 1, 1)
  val dvrRecord: DVRRecord =
    DVRRecord(dvrRequest.organisationId, dvrRequest.assessmentRef, dvrRequest.agents, Some(dvrRequest.submissionId), timestamp)

  "create" should {
    "add one dvrRecord" in {
      repository.create(dvrRequest).flatMap(_ => findAll()).futureValue.loneElement.copy(createdAtTimestamp = timestamp) shouldBe dvrRecord
    }
  }

  "find" should {
    "return a dvrRecord" when {
      "a dvrSubmissionId has not been defined" in {
        val noSubmissionId = dvrRecord.copy(dvrSubmissionId = None)

        insert(noSubmissionId)
          .flatMap(_ => repository.find(noSubmissionId.organisationId, noSubmissionId.assessmentRef))
          .futureValue
          .value shouldBe noSubmissionId
      }
      "a dvrSubmissionId has been defined" in {
        insert(dvrRecord)
          .flatMap(_ => repository.find(dvrRecord.organisationId, dvrRecord.assessmentRef))
          .futureValue
          .value shouldBe dvrRecord
      }
      "a dvrSubmission matches an agent orgId but not the IP orgId" in {
        insert(dvrRecord)
          .flatMap(_ => repository.find(dvrRecord.agents.flatMap(_.headOption).getOrElse(90L), dvrRecord.assessmentRef))
          .futureValue
          .value shouldBe dvrRecord
      }
    }
    "return nothing" when {
      "no record matching either IP or agent organisationId exists" in {
        val unrelatedOrgId = 13579L
        deleteAll()
          .flatMap(_ => repository.collection.insertOne(dvrRecord.copy(organisationId = unrelatedOrgId)).toFuture())
          .flatMap(_ => repository.find(dvrRequest.organisationId, dvrRequest.assessmentRef))
          .futureValue shouldBe None
      }
      "no record matching assessmentRef exists" in {
        val unrelatedAssessmentRef = 13579L
        deleteAll()
          .flatMap(_ =>
            repository.collection.insertOne(dvrRecord.copy(assessmentRef = unrelatedAssessmentRef)).toFuture())
          .flatMap(_ => repository.find(dvrRequest.organisationId, dvrRequest.assessmentRef))
          .futureValue shouldBe None
      }
    }
  }

  "clear" should {
    "remove an existing dvrRecord" in {
      def queryResult =
        find(
          Filters.and(
            Filters.eq("organisationId", dvrRequest.organisationId),
            Filters.eq("assessmentRef", dvrRequest.assessmentRef)
          )
        )

      val result = for {
        _               <- insert(dvrRecord)
        preClearResult  <- queryResult
        _               <- repository.clear(dvrRecord.organisationId)
        postClearResult <- queryResult
      } yield {
        preClearResult.loneElement shouldBe dvrRecord
        postClearResult shouldBe empty
      }
      result.futureValue
    }
  }
}
