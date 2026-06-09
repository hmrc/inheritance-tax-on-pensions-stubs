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

package uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.services.ResourceService

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class IhtpReportRetrieveController @Inject() (
  cc: ControllerComponents,
  resourceService: ResourceService
) extends IhtpControllerBase(cc) {

  def getIhtpReport: Action[AnyContent] = Action.async { implicit request =>
    val pstr = request.getQueryString("pstr")
    val fbNumber = request.getQueryString("fbNumber")
    val paymentReferenceNumber = request.getQueryString("paymentReferenceNumber")
    val versionNumber = request.getQueryString("versionNumber")

    pstr match {
      case None =>
        Future.successful(BadRequest(invalidPayload))
      case Some(_) =>
        (fbNumber, paymentReferenceNumber, versionNumber) match {
          case (Some(_), None, None) =>
            handleRetrieval(fbNumber.get)
          case (None, Some(prn), Some(vn)) =>
            handleRetrieval(s"${prn}_$vn")
          case (None, None, None) =>
            Future.successful(BadRequest(invalidPayload))
          case _ =>
            Future.successful(BadRequest(invalidPayload))
        }
    }
  }

  private def handleRetrieval(identifier: String)(implicit request: play.api.mvc.Request[?]): Future[Result] =
    resourceService.getResource("retrieve", identifier) match {
      case Some(json) =>
        Future.successful(
          withCorrelationId(Ok(json))
        )
      case None =>
        Future.successful(noRecordsFound)
    }

  private def withCorrelationId(result: Result)(implicit request: play.api.mvc.Request[?]): Result =
    result.withHeaders(request.headers.get("correlationid").map("correlationid" -> _).toSeq*)

  private def noRecordsFound(implicit request: play.api.mvc.Request[?]): Result =
    withCorrelationId(
      UnprocessableEntity(
        Json.obj(
          "errors" -> Json.obj(
            "processingDate" -> "2026-06-07T16:12:49Z",
            "code" -> "003",
            "text" -> "Request could not be processed"
          )
        )
      )
    )
}
