/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.libs.json.{JsPath, JsString}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportRetrieveController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.APIResponses
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportRetrieveControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportRetrieveController]
  private val correlationId = "d59434ad-6e01-4467-9209-66858e778736"

  private def retrieveRequest(queryString: String) = FakeRequest("GET", s"/$queryString").withHeaders(
    "correlationid" -> correlationId,
    "X-Message-Type" -> "Request",
    "X-Originating-System" -> "MDTP",
    "X-Receipt-Date" -> "2026-04-10T16:12:49Z",
    "X-Regime-Type" -> "IHTP",
    "X-Transmitting-System" -> "MDTP"
  )

  "GET ihtp report" must {

    "return 200-Ok for a known fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004320")
      )

      status(result) mustBe Status.OK
      header("correlationid", result).value mustBe correlationId

      val content = contentAsJson(result)
      (JsPath \ "reportDetails" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "fbNumber")(content) mustBe List(JsString("119000004320"))
      (JsPath \ "paymentReference")(content) mustBe List(JsString("PR000000001"))
      (JsPath \ "ihtpStatus")(content) mustBe List(JsString("Processed"))
    }

    "return 200-Ok for a second known fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000002IN&fbNumber=119000004322")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "reportDetails" \ "pstr")(content) mustBe List(JsString("24000002IN"))
      (JsPath \ "fbNumber")(content) mustBe List(JsString("119000004322"))
      (JsPath \ "paymentReference")(content) mustBe List(JsString("PR000000003"))
    }

    "return 200-Ok for known paymentReference and versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=PR000000001&versionNumber=001")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "reportDetails" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "fbNumber")(content) mustBe List(JsString("119000004320"))
      (JsPath \ "paymentReference")(content) mustBe List(JsString("PR000000001"))
      (JsPath \ "ihtpVersion")(content) mustBe List(JsString("001"))
    }

    "return 422-UnprocessableEntity for an unknown fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=999999999999")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      header("correlationid", result).value mustBe correlationId
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 422-UnprocessableEntity when pstr does not match the resource file's pstr" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000002IN&fbNumber=119000004320")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      header("correlationid", result).value mustBe correlationId
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 400-BadRequest for invalid parameter combination (fbNumber with paymentReferenceNumber)" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004320&paymentReferenceNumber=PR000000001")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 400-BadRequest when pstr is missing" in {
      val result = controller.getIhtpReport()(retrieveRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 400-BadRequest when both fbNumber and paymentReferenceNumber are missing" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 400-BadRequest when paymentReferenceNumber is provided without versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=PR000000001")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 400-BadRequest when versionNumber is provided without paymentReferenceNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&versionNumber=001")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }
  }
}
