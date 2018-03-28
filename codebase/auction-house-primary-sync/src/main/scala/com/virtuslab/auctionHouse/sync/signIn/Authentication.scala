package com.virtuslab.auctionHouse.sync.signIn

import com.virtuslab.auctionHouse.sync.cassandra.Account
import com.virtuslab.identity.{TokenValidationResponse, ValidateTokenRequest}
import com.virtuslab.{Config, RequestMetrics, TraceId}
import org.json4s.{DefaultFormats, Formats}
import org.json4s.jackson.Serialization.{read, write}
import org.scalatra.ScalatraBase
import scalaj.http._

trait Authentication extends ScalatraBase { this: RequestMetrics =>

  import org.json4s._

  private val AUTHORIZATION_KEYS = Seq(
    "Authorization",
    "HTTP_AUTHORIZATION",
    "X-HTTP_AUTHORIZATION",
    "X_HTTP_AUTHORIZATION"
  )

  private val identityUrl = s"http://${Config.identityServiceContactPoint}/api/v1/validate"

  protected implicit val jsonFormats: Formats = DefaultFormats

  def auth[T](fun: String => T)(implicit traceId: TraceId): T = {
    val histogramTimer = requestsLatency.labels("authenticate").startTimer()

    val maybeUsername = getToken.flatMap { requestedToken =>
      val body = write(ValidateTokenRequest(requestedToken))
      val response = Http(identityUrl)
        .headers(("X-Trace-Id", traceId.id) :: ("Content-Type", "application/json") :: Nil)
        .postData(body)
        .asString

      if (response.code == 200) {
        Some(read[TokenValidationResponse](response.body).username)
      } else {
        None
      }
    }

    histogramTimer.observeDuration()

    val username = maybeUsername getOrElse halt(status = 401)

    fun(username)
  }

  private def getToken: Option[String] = {
    AUTHORIZATION_KEYS.find(k => Option(request.getHeader(k)).isDefined)
      .map(request.getHeader(_).trim.split(" ", 2).map(_.trim))
      .find(arr => arr.length == 2 && arr(0).equalsIgnoreCase("bearer"))
      .map(_(1))
  }
}
