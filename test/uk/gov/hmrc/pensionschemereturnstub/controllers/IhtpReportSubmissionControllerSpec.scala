/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.pensionschemereturnstub.controllers

import org.scalacheck.Gen
import play.api.http.Status
import java.time.Instant
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportSubmissionController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.models.{AddressDetails, IhtpPaymentNoticeSubmission, PrContactDetails}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.{APIResponses, JsonUtils}
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportSubmissionControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportSubmissionController]
  private val jsonUtils = app.injector.instanceOf[JsonUtils]

  private def digits(length: Int): Gen[String] = Gen.listOfN(length, Gen.numChar).map(_.mkString)

  private val psaId = digits(7).map("A" + _).sample.get
  private val pspId = digits(8).sample.get
  private val inheritanceTaxReference = (for {
    prefix <- Gen.oneOf("A", "F")
    number <- digits(6)
    year <- digits(2)
  } yield s"$prefix$number/${year}A").sample.get

  private def submissionData(fileName: String): JsValue = {
    val data = jsonUtils.readJsonFile(s"conf/resources/data/$fileName.json")
    val referencePath = __ \ "ihtNoticeRequest" \ "deceased" \ "deceasedDetails" \ "ihtRefNumber"
    val declarationsPath = __ \ "ihtNoticeRequest" \ "declarations"
    // Preserve the suffix that selects the existing success or error scenario.
    val reference = inheritanceTaxReference.dropRight(1) + data.as[String](referencePath.read[String]).takeRight(1)
    val submitterId =
      if ((data \ "ihtNoticeRequest" \ "declarations" \ "submittedBy").as[String] == "PSA") psaId else pspId
    val updates = referencePath.json
      .update(Reads.pure(JsString(reference)))
      .andThen((declarationsPath \ "submitterId").json.update(Reads.pure(JsString(submitterId))))
    val updated = data.transform(updates).get

    if ((data \ "ihtNoticeRequest" \ "declarations" \ "pspDeclaration").toOption.isDefined) {
      updated.transform((declarationsPath \ "pspDeclaration" \ "psaid").json.update(Reads.pure(JsString(psaId)))).get
    } else updated
  }

  private val fakePostRequest = FakeRequest("POST", "/").withHeaders(
    ("CorrelationId", "testId"),
    "Authorization" -> "test Bearer token",
    ("Environment", "local")
  )

  "POST ihtp report" must {

    "return 201-Created for a valid request" in {
      val validData = submissionData("validReturnSubmission")
      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.CREATED
      val content = contentAsJson(result)
      (content \ "success" \ "ihtResponse" \ "processingDate").validate[Instant].isSuccess mustBe true
      (JsPath \ "success" \ "ihtResponse" \ "formBundleNo")(content) must not be empty
      val reference = (validData \ "ihtNoticeRequest" \ "deceased" \ "deceasedDetails" \ "ihtRefNumber").as[String]
      (content \ "success" \ "ihtResponse" \ "ihtPaymentReference").as[String] mustBe s"${reference}556789"
    }

    "return 201-Created for a valid organisation PR request" in {
      val validData = submissionData("validReturnSubmissionOrganisation")

      validData
        .validate[IhtpPaymentNoticeSubmission]
        .map(_.ihtNoticeRequest.personalRep.prContactDetails) mustBe JsSuccess(
        PrContactDetails(
          Some("Test Organisation"),
          Some("Mr"),
          "FirstnameA",
          Some("SecondnameB"),
          "Surname",
          AddressDetails(
            addressLine1 = "1 ABCDE Street",
            addressLine2 = Some("FGHIJ Town"),
            postCode = Some("ZZ99 1AA"),
            country = "GB"
          )
        )
      )

      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.CREATED
      val content = contentAsJson(result)
      (JsPath \ "success" \ "ihtResponse" \ "formBundleNo")(content) must not be empty
      val reference = (validData \ "ihtNoticeRequest" \ "deceased" \ "deceasedDetails" \ "ihtRefNumber").as[String]
      (content \ "success" \ "ihtResponse" \ "ihtPaymentReference").as[String] mustBe s"${reference}556789"
    }

    "return 400-BadRequest for an organisation request missing organisation name" in {
      val invalidData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> inheritanceTaxReference,
          "firstForename" -> "Firstname",
          "surname" -> "Surname",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonNoNino" -> "Reason for no national insurance number"
        ),
        "prDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "title" -> "Ms",
            "firstForename" -> "FirstnameA",
            "secondForename" -> "Ann",
            "surname" -> "Surname"
          )
        )
      )
      invalidData.validate[IhtpPaymentNoticeSubmission] mustBe a[JsError]

      val postRequest = fakePostRequest.withJsonBody(invalidData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for an organisation request missing PR name fields" in {
      val invalidData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> inheritanceTaxReference,
          "firstForename" -> "Firstname",
          "surname" -> "Surname",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonNoNino" -> "Reason for no national insurance number"
        ),
        "prDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "organisationName" -> "Test Organisation"
          )
        )
      )
      val postRequest = fakePostRequest.withJsonBody(invalidData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for a missing json body" in {
      val result = controller.postIhtpReport()(fakePostRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest for an invalid srn" in {
      val badRequestData = submissionData("BadRequestSubmission")
      val postRequest = fakePostRequest.withJsonBody(badRequestData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.BAD_REQUEST
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Submission has not passed validation. Invalid parameter srn.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INVALID_SRN"))
    }

    "return 500-InternalServerError for an srn" in {
      val serverErrorData = submissionData("ServerErrorSubmission")
      val postRequest = fakePostRequest.withJsonBody(serverErrorData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Something went wrong.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INTERNAL_SERVER_ERROR"))
    }

    "return 503-ServiceUnavailable for an srn" in {
      val unavailableData = submissionData("UnavailableSubmission")
      val postRequest = fakePostRequest.withJsonBody(unavailableData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.SERVICE_UNAVAILABLE
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("The remote endpoint has indicated that the service is unavailable")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("SERVICE_UNAVAILABLE"))
    }

    "return 422-UnprocessableEntity for an srn" in {
      val unprocessableData = submissionData("UnprocessableSubmission")
      val postRequest = fakePostRequest.withJsonBody(unprocessableData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("The remote endpoint returned unprocessable")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("UNPROCESSABLE_ENTITY"))
    }
  }
}
