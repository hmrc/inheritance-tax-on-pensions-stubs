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

import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.services.ResourceService

import javax.inject.{Inject, Singleton}
import java.time.{LocalDate, OffsetDateTime}
import scala.concurrent.Future
import scala.util.Try

@Singleton()
class IhtpReportOverviewController @Inject() (
  cc: ControllerComponents,
  resourceService: ResourceService
) extends IhtpControllerBase(cc) {

  def getIhtpOverview: Action[AnyContent] = Action.async { implicit request =>
    (request.getQueryString("pstr"), request.getQueryString("dateFrom"), request.getQueryString("dateTo")) match {
      case (Some(pstr), Some(dateFrom), Some(dateTo)) =>
        scenarioResponse(request.getQueryString("status")).getOrElse {
          val maybeOverview = resourceService
            .getResource("overview", pstr)
            .map(json => (json \ "success" \ "ihtpOverview").asOpt[Seq[JsObject]].getOrElse(Seq.empty))

          maybeOverview match {
            case Some(overview) =>
              val filteredByDate = overview.filter(item => isInDateRange(item, dateFrom, dateTo))
              val filteredOverview = request
                .getQueryString("status")
                .fold(filteredByDate)(status =>
                  filteredByDate.filter(item => (item \ "ihtpStatus").as[String] == status)
                )

              Future.successful(
                if (filteredOverview.isEmpty) {
                  noRecordsFound
                } else {
                  withCorrelationId(
                    Ok(
                      Json.obj(
                        "success" -> Json.obj(
                          "ihtpOverview" -> JsArray(filteredOverview)
                        )
                      )
                    )
                  )
                }
              )
            case None =>
              Future.successful(noRecordsFound)
          }
        }
      case _ =>
        Future.successful(BadRequest(hodBadRequestResponse))
    }
  }

  private def scenarioResponse(
    status: Option[String]
  )(implicit request: play.api.mvc.Request[?]): Option[Future[Result]] =
    status.map(_.trim.toUpperCase).flatMap {
      case "BAD_REQUEST" => Some(Future.successful(BadRequest(hodBadRequestResponse)))
      case "SERVER_ERROR" => Some(internalServerError500Response)
      case "SERVICE_UNAVAILABLE" => Some(serviceUnavailable503Response)
      case "NO_RECORDS" => Some(Future.successful(noRecordsFound))
      case _ => None
    }

  private def withCorrelationId(result: Result)(implicit request: play.api.mvc.Request[?]): Result =
    result.withHeaders(request.headers.get("correlationid").map("correlationid" -> _).toSeq*)

  private def noRecordsFound(implicit request: play.api.mvc.Request[?]): Result =
    withCorrelationId(
      UnprocessableEntity(
        Json.obj(
          "errors" -> Json.obj(
            "processingDate" -> "2026-04-10T16:12:49Z",
            "code" -> "003",
            "text" -> "Request could not be processed"
          )
        )
      )
    )

  private def isInDateRange(item: JsObject, dateFrom: String, dateTo: String): Boolean =
    (for {
      from <- Try(LocalDate.parse(dateFrom)).toOption
      to <- Try(LocalDate.parse(dateTo)).toOption
      submissionDate <- (item \ "submissionDate").asOpt[String]
      submissionLocalDate <- Try(OffsetDateTime.parse(submissionDate).toLocalDate).toOption
    } yield !submissionLocalDate.isBefore(from) && !submissionLocalDate.isAfter(to)).getOrElse(false)
}
