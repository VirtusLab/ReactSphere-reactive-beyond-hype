package com.virtuslab.base.sync

import com.virtuslab.identity.{TokenValidationResponse, ValidateTokenRequest}
import com.virtuslab._
import org.json4s.jackson.Serialization.{read, write}
import org.scalatra.ScalatraBase

trait Authentication extends ScalatraBase with TraceIdSupport with HeadersSupport { this: RequestMetrics with Logging =>

  import org.json4s._

  private val identityUrl = s"http://${Config.identityServiceContactPoint}/api/v1/validate"
  log.info(s"Identity url is: ${identityUrl}")

  protected implicit val jsonFormats: Formats = DefaultFormats

  def auth[T](fun: String => T)(implicit traceId: TraceId): T = {
    val histogramTimer = requestsLatency.labels("authenticate").startTimer()

    val maybeUsername = getToken.flatMap { requestedToken =>
      val body = write(ValidateTokenRequest(requestedToken.value))
      val response = Http(identityUrl)
        .headers(traceHeaders)
        .postData(body)
        .asString

      if (response.code == 200) {
        Some(read[TokenValidationResponse](response.body).username)
      } else {
        None
      }
    }

    histogramTimer.observeDuration()

    val username = maybeUsername.getOrElse {
      log.warn(s"Request cannot be authenticated, token is: ${getToken.getOrElse("<empty>")}")
      halt(status = 401)
    }

    fun(username)
  }

  implicit protected def getToken: Option[AuthToken] = {
    AUTHORIZATION_KEYS.find(k => Option(request.getHeader(k)).isDefined)
      .map(request.getHeader(_).trim.split(" ", 2).map(_.trim))
      .find(arr => arr.length == 2 && arr(0).equalsIgnoreCase("bearer"))
      .map { arr =>
        AuthToken(arr(1))
      }
  }
}
