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

    val srn = "S2400000001"

    "return 200-Ok for a valid request" in {
      val validData = jsonUtils.readJsonFile(filePath = "conf/resources/data/validReturnSubmission.json")
      val postRequest = fakePostRequest.withJsonBody(validData)

      val result = controller.postIhtpReport(srn)(postRequest)
      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "formBundleNumber")(content) must not be empty
      (JsPath \ "processingDateTime")(content) must not be empty
    }

    "return 400-BadRequest for a missing json body" in {
      val result = controller.postIhtpReport(srn)(fakePostRequest)
      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 400-BadRequest for an invalid srn" in {
      val result = controller.postIhtpReport("S2400000011")(fakePostRequest)
      status(result) mustBe Status.BAD_REQUEST
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Submission has not passed validation. Invalid parameter srn.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INVALID_SRN"))
    }

    "return 500-InternalServerError for an srn" in {
      val result = controller.postIhtpReport("S2400000012")(fakePostRequest)
      status(result) mustBe Status.INTERNAL_SERVER_ERROR
      val content = contentAsJson(result)
      (JsPath \ "failures" \ 0 \ "reason")(content) mustBe List(
        JsString("Something went wrong.")
      )
      (JsPath \ "failures" \ 0 \ "code")(content) mustBe List(JsString("INTERNAL_SERVER_ERROR"))
    }
  }
}
