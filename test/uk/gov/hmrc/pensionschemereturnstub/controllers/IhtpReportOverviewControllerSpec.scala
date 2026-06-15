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
import uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers.IhtpReportOverviewController
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.APIResponses
import uk.gov.hmrc.pensionschemereturnstub.base.SpecBase

class IhtpReportOverviewControllerSpec extends SpecBase with APIResponses {

  private val controller = app.injector.instanceOf[IhtpReportOverviewController]
  private val correlationId = "d59434ad-6e01-4467-9209-66858e778736"

  private def overviewRequest(queryString: String) = FakeRequest("GET", s"/$queryString").withHeaders(
    "correlationid" -> correlationId,
    "X-Message-Type" -> "Request",
    "X-Originating-System" -> "MDTP",
    "X-Receipt-Date" -> "2026-04-10T16:12:49Z",
    "X-Regime-Type" -> "IHTP",
    "X-Transmitting-System" -> "MDTP"
  )

  "GET ihtp overview" must {

    "return 200-Ok for a known pstr with overview items" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      header("correlationid", result).value mustBe correlationId

      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "fbNumber")(content) mustBe List(JsString("119000004320"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "firstForename")(content) mustBe List(JsString("John"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "surname")(content) mustBe List(JsString("Doe"))
    }

    "return 200-Ok for a second known pstr" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe List(JsString("24000002IN"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "paymentReference")(content) mustBe List(JsString("A654321/25A392617"))
    }

    "return 200-Ok with only matching items when status is supplied" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=Submitted")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "ihtpStatus")(content) mustBe List(JsString("Submitted"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "paymentReference")(content) mustBe List(JsString("A123456/25F482603"))
    }

    "return 422-UnprocessableEntity when no overview items match the supplied date range" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2027-01-01&dateTo=2027-12-31")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 422-UnprocessableEntity when status is NO_RECORDS" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=NO_RECORDS")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
      (JsPath \ "errors" \ "text")(contentAsJson(result)) mustBe List(JsString("Request could not be processed"))
    }

    "return 400-BadRequest when status is BAD_REQUEST" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=BAD_REQUEST")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }

    "return 500-InternalServerError when status is SERVER_ERROR" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=SERVER_ERROR")
      )

      status(result) mustBe Status.INTERNAL_SERVER_ERROR
      (JsPath \ "failures" \ 0 \ "code")(contentAsJson(result)) mustBe List(JsString("INTERNAL_SERVER_ERROR"))
    }

    "return 503-ServiceUnavailable when status is SERVICE_UNAVAILABLE" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=SERVICE_UNAVAILABLE")
      )

      status(result) mustBe Status.SERVICE_UNAVAILABLE
      (JsPath \ "failures" \ 0 \ "code")(contentAsJson(result)) mustBe List(JsString("SERVICE_UNAVAILABLE"))
    }

    "return 422-UnprocessableEntity for an unknown pstr" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000003IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.UNPROCESSABLE_ENTITY
      header("correlationid", result).value mustBe correlationId
      (JsPath \ "errors" \ "code")(contentAsJson(result)) mustBe List(JsString("003"))
    }

    "return 400-BadRequest when mandatory query params are missing" in {
      val result = controller.getIhtpOverview()(overviewRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe invalidPayload
    }
  }
}
