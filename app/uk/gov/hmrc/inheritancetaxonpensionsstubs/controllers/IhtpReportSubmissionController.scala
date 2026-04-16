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

package uk.gov.hmrc.inheritancetaxonpensionsstubs.controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.config.Constants._
import uk.gov.hmrc.inheritancetaxonpensionsstubs.models.IhtpReportSubmissionPayload

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.{Success, Try}

@Singleton()
class IhtpReportSubmissionController @Inject() (
  cc: ControllerComponents
) extends IhtpControllerBase(cc) {

  private val logger = Logger(classOf[IhtpReportSubmissionController])

  def postIhtpReport(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson match {
      case Some(body) =>
        logger.info(message = s"postIhtpReport - Incoming payload: \n${Json.prettyPrint(body)}\n")

        Try(body.as[IhtpReportSubmissionPayload]) match {
          case Success(submissionResponse) =>
            val significantChar: String = submissionResponse.deceasedDetails.inheritanceTaxReference.takeRight(1)

            if (significantChar == BAD_REQUEST_CHAR) {
              invalidSrn400Response
            } else if (significantChar == SERVER_ERROR_CHAR) {
              internalServerError500Response
            } else if (significantChar == SERVICE_UNAVAILABLE_CHAR) {
              serviceUnavailable503Response
            } else if (significantChar == UNPROCESSABLE_ENTITY_CHAR) {
              unprocessable422Response
            } else {
              Future.successful(
                Ok(
                  Json.obj(
                    "processingDateTime" -> LocalDateTime
                      .now()
                      .atOffset(ZoneOffset.UTC)
                      .format(DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssX")),
                    "formBundleNumber" -> "000012345678",
                    "paymentReference" -> "000012345321"
                  )
                )
              )
            }
          case _ =>
            logger.debug("Could not parse body -> Bad request")
            Future.successful(BadRequest(invalidPayload))
        }
      case _ =>
        logger.debug("No body -> Bad request")
        Future.successful(BadRequest(invalidPayload))
    }
  }
}
