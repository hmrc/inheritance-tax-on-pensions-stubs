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
import play.api.libs.json.{JsBoolean, JsPath, JsString, JsValue}
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
      (JsPath \ "success" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "success" \ "processingDate")(content) must not be empty
      (JsPath \ "success" \ "ihtpDetails" \ "status")(content) mustBe List(JsString("Submitted"))
      (JsPath \ "success" \ "ihtpDetails" \ "version")(content) mustBe List(JsString("001"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "paymentReference")(content) mustBe empty
      (JsPath \ "success" \ "ihtpDeclaration")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "prDetails")(content) must not be empty
      (JsPath \ "success" \ "prDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))

      (JsPath \ "success" \ "ihtTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihtTaxInformation" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails")(content) must not be empty
      (JsPath \ "success" \ "beneficiaryDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails" \ "beneficiaries")(content) must not be empty
      (JsPath \ "success" \ "psaDeclarations")(content) must not be empty
    }

    "return 200-Ok for a second known fbNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000002IN&fbNumber=119000004322")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe List(JsString("24000002IN"))
      (JsPath \ "success" \ "processingDate")(content) must not be empty
      (JsPath \ "success" \ "ihtpDetails" \ "status")(content) mustBe List(JsString("Processed"))
      (JsPath \ "success" \ "ihtpDetails" \ "version")(content) mustBe List(JsString("001"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "paymentReference")(content) mustBe empty

      (JsPath \ "success" \ "ihtpDeclaration")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "prDetails")(content) must not be empty
      (JsPath \ "success" \ "prDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))

      (JsPath \ "success" \ "ihtTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihtTaxInformation" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails")(content) must not be empty
      (JsPath \ "success" \ "beneficiaryDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails" \ "beneficiaries")(content) must not be empty
      (JsPath \ "success" \ "pspDeclarations")(content) must not be empty // PSP
    }

    "return 200-Ok for known paymentReference and versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A123456/25A-629671&versionNumber=001")
      )

      status(result) mustBe Status.OK
      val content = contentAsJson(result)
      (JsPath \ "success" \ "pstr")(content) mustBe List(JsString("24000001IN"))
      (JsPath \ "success" \ "processingDate")(content) must not be empty
      (JsPath \ "success" \ "ihtpDetails" \ "status")(content) mustBe List(JsString("Submitted"))
      (JsPath \ "success" \ "ihtpDetails" \ "version")(content) mustBe List(JsString("001"))

      (JsPath \ "fbNumber")(content) mustBe empty
      (JsPath \ "paymentReference")(content) mustBe empty

      (JsPath \ "success" \ "ihtpDeclaration")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails")(content) must not be empty
      (JsPath \ "success" \ "deceasedDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "prDetails")(content) must not be empty
      (JsPath \ "success" \ "prDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))

      (JsPath \ "success" \ "ihtTaxInformation")(content) must not be empty
      (JsPath \ "success" \ "ihtTaxInformation" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails")(content) must not be empty
      (JsPath \ "success" \ "beneficiaryDetails" \ "changeFlag")(content) mustBe List(JsBoolean(false))
      (JsPath \ "success" \ "beneficiaryDetails" \ "beneficiaries")(content) must not be empty
      (JsPath \ "success" \ "psaDeclarations")(content) must not be empty
    }

    "return the pinned and amended versions of a report by fbNumber" in {
      val versionOneResult = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004360")
      )
      val versionTwoResult = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004361")
      )

      status(versionOneResult) mustBe Status.OK
      status(versionTwoResult) mustBe Status.OK

      val versionOne = contentAsJson(versionOneResult)
      val versionTwo = contentAsJson(versionTwoResult)

      (versionOne \ "success" \ "ihtpDetails" \ "version").as[String] mustBe "001"
      (versionOne \ "success" \ "ihtpDetails" \ "status").as[String] mustBe "Paid"
      (versionTwo \ "success" \ "ihtpDetails" \ "version").as[String] mustBe "002"
      (versionTwo \ "success" \ "ihtpDetails" \ "status").as[String] mustBe "Submitted"

      (versionOne \ "success" \ "deceasedDetails" \ "inheritanceTaxReference").as[String] mustBe
        (versionTwo \ "success" \ "deceasedDetails" \ "inheritanceTaxReference").as[String]

      (versionOne \ "success" \ "deceasedDetails" \ "changeFlag").as[Boolean] mustBe false
      (versionOne \ "success" \ "prDetails" \ "changeFlag").as[Boolean] mustBe false
      (versionOne \ "success" \ "ihtTaxInformation" \ "changeFlag").as[Boolean] mustBe false
      (versionOne \ "success" \ "beneficiaryDetails" \ "changeFlag").as[Boolean] mustBe false

      (versionTwo \ "success" \ "deceasedDetails" \ "changeFlag").as[Boolean] mustBe false
      (versionTwo \ "success" \ "prDetails" \ "changeFlag").as[Boolean] mustBe false
      (versionTwo \ "success" \ "ihtTaxInformation" \ "changeFlag").as[Boolean] mustBe true
      (versionTwo \ "success" \ "beneficiaryDetails" \ "changeFlag").as[Boolean] mustBe true
      (versionTwo \ "success" \ "beneficiaryDetails" \ "beneficiaries")
        .as[Seq[JsValue]]
        .map(beneficiary => (beneficiary \ "changeFlag").as[Boolean]) mustBe Seq(true, false)

      (versionOne \ "success" \ "ihtTaxInformation" \ "total").as[String] mustBe "110.00"
      (versionTwo \ "success" \ "ihtTaxInformation" \ "total").as[String] mustBe "132.00"
    }

    "return each amendment version by paymentReference and versionNumber" in {
      val versionOneByFbNumber = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004360")
      )
      val versionOneByPaymentReference = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A556789/26A-758204&versionNumber=001")
      )
      val versionTwoByFbNumber = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004361")
      )
      val versionTwoByPaymentReference = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A556789/26A-758204&versionNumber=002")
      )

      status(versionOneByPaymentReference) mustBe Status.OK
      status(versionTwoByPaymentReference) mustBe Status.OK
      contentAsJson(versionOneByPaymentReference) mustBe contentAsJson(versionOneByFbNumber)
      contentAsJson(versionTwoByPaymentReference) mustBe contentAsJson(versionTwoByFbNumber)
    }

    "return the additional paid version 001 reports by fbNumber and paymentReference" in {
      Seq(
        ("119000004362", "F246810/26B-314159"),
        ("119000004363", "A975310/26C-271828")
      ).foreach { case (fbNumber, paymentReference) =>
        val byFbNumber = controller.getIhtpReport()(
          retrieveRequest(s"?pstr=24000001IN&fbNumber=$fbNumber")
        )
        val byPaymentReference = controller.getIhtpReport()(
          retrieveRequest(
            s"?pstr=24000001IN&paymentReferenceNumber=$paymentReference&versionNumber=001"
          )
        )

        status(byFbNumber) mustBe Status.OK
        status(byPaymentReference) mustBe Status.OK
        contentAsJson(byPaymentReference) mustBe contentAsJson(byFbNumber)

        val content = contentAsJson(byFbNumber)
        (content \ "success" \ "ihtpDetails" \ "version").as[String] mustBe "001"
        (content \ "success" \ "ihtpDetails" \ "status").as[String] mustBe "Paid"
      }
    }

    "return only false change flags for every version 001 report" in {
      Seq(
        "?pstr=24000001IN&fbNumber=119000004320",
        "?pstr=24000002IN&fbNumber=119000004322",
        "?pstr=24000001IN&fbNumber=119000004360",
        "?pstr=24000001IN&fbNumber=119000004362",
        "?pstr=24000001IN&fbNumber=119000004363",
        "?pstr=24000001IN&paymentReferenceNumber=A123456/25A-629671&versionNumber=001",
        "?pstr=24000001IN&paymentReferenceNumber=A556789/26A-758204&versionNumber=001"
      ).foreach { queryString =>
        val result = controller.getIhtpReport()(retrieveRequest(queryString))

        status(result) mustBe Status.OK
        val content = contentAsJson(result)
        (content \ "success" \ "ihtpDetails" \ "version").as[String] mustBe "001"

        val changeFlags = content \\ "changeFlag"
        changeFlags must not be empty
        changeFlags.foreach(_ mustBe JsBoolean(false))
      }
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
        retrieveRequest("?pstr=24000001IN&fbNumber=119000004320&paymentReferenceNumber=A123456/25A-629671")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when pstr is missing" in {
      val result = controller.getIhtpReport()(retrieveRequest(""))

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when both fbNumber and paymentReferenceNumber are missing" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when paymentReferenceNumber is provided without versionNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&paymentReferenceNumber=A123456/25A-629671")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }

    "return 400-BadRequest when versionNumber is provided without paymentReferenceNumber" in {
      val result = controller.getIhtpReport()(
        retrieveRequest("?pstr=24000001IN&versionNumber=001")
      )

      status(result) mustBe Status.BAD_REQUEST
      contentAsJson(result) mustBe hodBadRequestResponse
    }
  }
}
