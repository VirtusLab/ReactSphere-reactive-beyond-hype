package com.virtuslab.auctionHouse.identitySync.validation

import com.virtuslab.base.sync.BaseServlet
import com.virtuslab.identity.{TokenValidationResponse, ValidateTokenRequest}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{Ok, Unauthorized}

import scala.util.Try

class ValidationServlet extends BaseServlet {

  override def servletName: String = "Validation"

  override protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  lazy val validationService = new ValidationService()

  post("/") {
    val traceId = getTraceId
    val histogramTimer = requestsLatency.labels("validateTokenRequest").startTimer()
    val validateTokenRequest = parsedBody.extract[ValidateTokenRequest]

    logger.info(s"[${traceId.id}] Received validate token request...")

    val attempt = Try(validationService.validateToken(validateTokenRequest))
      .map {
        case Some(username) =>
          logger.info(s"[${traceId.id}] Successful validation of token, responding with user name '$username'.")
          Ok(TokenValidationResponse(username))
        case None =>
          logger.warn(s"[${traceId.id}] Token validation failure, responding with 401 Unauthorized.")
          Unauthorized()
      }

    if (attempt.isFailure) {
      logger.error(s"[${traceId.id}] Error occured while validating token:", attempt.failed.get)
    }

    histogramTimer.observeDuration()

    attempt.get
  }

}
