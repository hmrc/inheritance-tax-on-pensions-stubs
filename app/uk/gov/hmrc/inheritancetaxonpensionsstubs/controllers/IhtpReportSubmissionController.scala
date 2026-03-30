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

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton()
class IhtpReportSubmissionController @Inject()(
  cc: ControllerComponents,
) extends IhtpControllerBase(cc) {
  private val logger = Logger(classOf[IhtpReportSubmissionController])

  private val invalidPostSrn: String = "S2400000011"
  private val serverErrorSrn: String = "S2400000012"

  def postIhtpReport(srn: String): Action[AnyContent] = Action.async { implicit request =>
    if (srn == invalidPostSrn) {
      logger.debug("Invalid srn -> Bad request")
      invalidSrn400Response
    } else if (srn == serverErrorSrn) {
      logger.debug("Server error srn -> Server error")
      internalServerError500Response
    } else {
      request.body.asJson match {
        case Some(body) =>
          logger.info(message = s"postIhtpReport - Incoming payload: \n${Json.prettyPrint(body)}\n")
          logger.debug("postIhtpReport stubbed success -> Created")
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
        case _ =>
          logger.debug("No body -> Bad request")
          Future.successful(BadRequest(invalidPayload))
      }
    }
  }
}
