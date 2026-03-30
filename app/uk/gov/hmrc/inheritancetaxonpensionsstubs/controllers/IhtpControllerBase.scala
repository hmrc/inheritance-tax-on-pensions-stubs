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

import play.api.libs.json.Json
import play.api.mvc.{ControllerComponents, Result}
import uk.gov.hmrc.inheritancetaxonpensionsstubs.utils.APIResponses
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.Future

class IhtpControllerBase @Inject() (cc: ControllerComponents) extends BackendController(cc) with APIResponses {

  def unprocessable(code: String, reason: String): Future[Result] =
    Future.successful(
      UnprocessableEntity(
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> code,
              "reason" -> reason
            )
          )
        )
      )
    )

  private def badRequest(code: String, reason: String): Future[Result] =
    Future.successful(
      BadRequest(
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> code,
              "reason" -> reason
            )
          )
        )
      )
    )

  private def notFound(code: String, reason: String): Future[Result] =
    Future.successful(
      NotFound(
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> code,
              "reason" -> reason
            )
          )
        )
      )
    )

  private def serviceUnavailable(code: String, reason: String): Future[Result] =
    Future.successful(
      ServiceUnavailable(
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> code,
              "reason" -> reason
            )
          )
        )
      )
    )

  private def internalServerError(code: String, reason: String): Future[Result] =
    Future.successful(
      InternalServerError(
        Json.obj(
          "failures" -> Json.arr(
            Json.obj(
              "code" -> code,
              "reason" -> reason
            )
          )
        )
      )
    )

  // 400 - Bad request:
  val invalidSrn400Response: Future[Result] =
    badRequest("INVALID_SRN", "Submission has not passed validation. Invalid parameter srn.")

  // 404 - Not found
  val notFound404Response: Future[Result] =
    notFound("NO_REPORT_FOUND", "The remote endpoint has indicated No Scheme report was found.")

  // 503 - service unavailable
  val serviceUnavailable503Response: Future[Result] =
    serviceUnavailable(
      "SERVICE_UNAVAILABLE",
      "The remote endpoint has indicated that the service is unavailable"
    )

  // 500 - internal server error
  val internalServerError500Response: Future[Result] =
    internalServerError(
      "INTERNAL_SERVER_ERROR",
      "Something went wrong."
    )
}
