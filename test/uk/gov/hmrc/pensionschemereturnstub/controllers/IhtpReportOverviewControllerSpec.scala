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
import play.api.libs.json.{JsNull, JsPath, JsString, JsValue}
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
      (JsPath \ "success" \ "pstr")(content) mustBe empty
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "fbNumber")(content) mustBe List(JsString("119000004320"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "firstForename")(content) mustBe List(JsString("View"))
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "surname")(content) mustBe List(JsString("Change"))
    }

    "return 200-Ok for a second known pstr" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe empty
      (JsPath \ "success" \ "ihtpOverview" \ 0 \ "ihtPaymentReference")(content) mustBe List(
        JsString("A654321/25A999999")
      )
    }

    "return related report versions for an amendment scenario" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val amendmentReports = (contentAsJson(result) \ "success" \ "ihtpOverview")
        .as[Seq[JsValue]]
        .filter(report => (report \ "ihtPaymentReference").asOpt[String].contains("A556789/26A999999"))

      amendmentReports.size mustBe 2
      amendmentReports.map(report => (report \ "ihtVersion").as[String]) mustBe Seq("001", "002")
      amendmentReports.map(report => (report \ "fbNumber").as[String]).distinct.size mustBe 2
      amendmentReports.map(report => (report \ "ihtPaymentReference").as[String]).distinct mustBe Seq(
        "A556789/26A999999"
      )
    }

    "return two amendment scenarios with consistent payment statuses for each IHTP-574 scheme" in {
      Seq(
        "24000036IN" -> Map(
          "A240036/26A836241" -> "Paid",
          "F360024/26B472915" -> "Not reconciled"
        ),
        "00000042IN" -> Map(
          "A000042/26C604218" -> "Paid",
          "F420000/26D195307" -> "Not reconciled"
        )
      ).foreach { case (pstr, expectedStatuses) =>
        val result = controller.getIhtpOverview()(
          overviewRequest(s"?pstr=$pstr&dateFrom=2026-01-01&dateTo=2026-12-31")
        )

        status(result) mustBe Status.OK
        val reports = (contentAsJson(result) \ "success" \ "ihtpOverview").as[Seq[JsValue]]
        val amendmentScenarios = reports
          .groupBy(report => (report \ "ihtPaymentReference").as[String])
          .values
          .filter(_.size == 2)
          .toSeq

        amendmentScenarios.size mustBe 2
        amendmentScenarios.foreach { versions =>
          versions.map(report => (report \ "ihtVersion").as[String]).sorted mustBe Seq("001", "002")
          versions.map(report => (report \ "fbNumber").as[String]).distinct.size mustBe 2
          versions.map(report => (report \ "ihtPaymentReference").as[String]).distinct.size mustBe 1

          val paymentReference = (versions.head \ "ihtPaymentReference").as[String]
          versions.map(report => (report \ "ihtStatus").as[String]).distinct mustBe
            Seq(expectedStatuses(paymentReference))
          versions.foreach(report => (report \ "nino").toOption mustBe None)
        }
      }
    }

    "return two additional paid version 001 reports" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31")
      )

      status(result) mustBe Status.OK
      val paidReports = (contentAsJson(result) \ "success" \ "ihtpOverview")
        .as[Seq[JsValue]]
        .filter(report =>
          (report \ "ihtStatus").as[String] == "Paid" &&
            (report \ "ihtVersion").as[String] == "001"
        )

      paidReports
        .map(report => (report \ "fbNumber").as[String]) mustBe Seq("119000004360", "119000004362", "119000004363")
      paidReports.map(report => (report \ "ihtPaymentReference").as[String]) mustBe
        Seq("A556789/26A999999", "F246810/26B999999", "A975310/26C999999")
    }

    "return 200 and empty list when no overview items match the supplied date range" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000002IN&dateFrom=2027-01-01&dateTo=2027-12-31")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      val reports = (content \ "success" \ "ihtpOverview").as[Seq[JsValue]]
      reports.size mustBe 0
    }

    "return 200 and empty list when status is NO_RECORDS" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=NO_RECORDS")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      val reports = (content \ "success" \ "ihtpOverview").as[Seq[JsValue]]
      reports.size mustBe 0
    }

    "return 400-BadRequest when status is BAD_REQUEST" in {
      val result = controller.getIhtpOverview()(
        overviewRequest("?pstr=24000001IN&dateFrom=2026-01-01&dateTo=2026-12-31&status=BAD_REQUEST")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
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

    "return 400-BadRequest when mandatory query params are missing" in {
      val result = controller.getIhtpOverview()(overviewRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }
  }
}
