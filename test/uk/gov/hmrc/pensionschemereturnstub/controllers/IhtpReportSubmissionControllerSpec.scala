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

import play.api.http.Status
import play.api.libs.json.*
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportSubmissionController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.models.{IhtpReportSubmissionPayload, OrganisationDetails}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.{APIResponses, JsonUtils}
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportSubmissionControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportSubmissionController]
  private val jsonUtils = app.injector.instanceOf[JsonUtils]

  private val fakePostRequest = FakeRequest("POST", "/").withHeaders(
    ("CorrelationId", "testId"),
    "Authorization" -> "test Bearer token",
    ("Environment", "local")
  )

  "POST ihtp report" must {

    "return 200-Ok for a valid request" in {
      val validData = jsonUtils.readJsonFile(filePath = "conf/resources/data/validReturnSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "formBundleNumber")(content) must not be empty
      (JsPath \ "processingDateTime")(content) must not be empty
    }

    "return 200-Ok for a valid organisation PR request" in {
      val validData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> "A123456/25A",
          "title" -> "Mr",
          "firstForename" -> "John",
          "secondForename" -> "William",
          "surname" -> "Doe",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonForNoNino" -> "Reason for no national insurance number"
        ),
        "lprDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "organisationName" -> "Test Organisation",
            "title" -> "Ms",
            "firstForename" -> "Jane",
            "secondForename" -> "Ann",
            "surname" -> "Doe"
          )
        ),
        "ihtTaxInformation" -> Json.obj(
          "dateThePensionSchemeReceivedNoticeToPay" -> "2026-03-27",
          "didTheLegalPersonalRepresentativeSubmitTheNotice" -> "Yes"
        )
      )
      validData.validate[IhtpReportSubmissionPayload].map(_.lprDetails.organisation) mustBe JsSuccess(
        Some(OrganisationDetails("Test Organisation", Some("Ms"), "Jane", Some("Ann"), "Doe"))
      )

      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport()(postRequest)
      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "formBundleNumber")(content) must not be empty
      (JsPath \ "processingDateTime")(content) must not be empty
    }

    "return 400-BadRequest for an organisation request missing organisation name" in {
      val invalidData = Json.obj(
        "reportDetails" -> Json.obj(
          "pstr" -> "S2400000001"
        ),
        "deceasedDetails" -> Json.obj(
          "inheritanceTaxReference" -> "A123456/25A",
          "firstForename" -> "John",
          "surname" -> "Doe",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonForNoNino" -> "Reason for no national insurance number"
        ),
        "lprDetails" -> Json.obj(
          "organisation" -> Json.obj(
            "title" -> "Ms",
            "firstForename" -> "Jane",
            "secondForename" -> "Ann",
            "surname" -> "Doe"
          )
        )
      )
      invalidData.validate[IhtpReportSubmissionPayload] mustBe a[JsError]

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
          "inheritanceTaxReference" -> "A123456/25A",
          "firstForename" -> "John",
          "surname" -> "Doe",
          "dateOfBirth" -> "1950-01-01",
          "dateOfDeath" -> "2026-01-01",
          "reasonForNoNino" -> "Reason for no national insurance number"
        ),
        "lprDetails" -> Json.obj(
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
      val badRequestData = jsonUtils.readJsonFile(filePath = "conf/resources/data/BadRequestSubmission.json")
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
      val serverErrorData = jsonUtils.readJsonFile(filePath = "conf/resources/data/ServerErrorSubmission.json")
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
      val unavailableData = jsonUtils.readJsonFile(filePath = "conf/resources/data/UnavailableSubmission.json")
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
      val unprocessableData = jsonUtils.readJsonFile(filePath = "conf/resources/data/UnprocessableSubmission.json")
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
