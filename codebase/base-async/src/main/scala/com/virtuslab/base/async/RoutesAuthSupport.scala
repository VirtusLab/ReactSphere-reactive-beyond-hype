package com.virtuslab.base.async

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Forbidden, Unauthorized}
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives.{authenticateOAuth2Async, complete}
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directive1, MissingQueryParamRejection, RejectionHandler}
import com.virtuslab.{Logging, TraceId}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.concurrent.Future

trait RoutesAuthSupport extends SprayJsonSupport with DefaultJsonProtocol with IdentityHelpers {
  this: Logging =>

  type AuthFunction = Credentials => Future[Option[String]]

  implicit lazy val iteFormat: RootJsonFormat[InvalidTokenError] = jsonFormat1(InvalidTokenError)
  implicit lazy val mteFormat: RootJsonFormat[MissingTokenError] = jsonFormat1(MissingTokenError)

  def rejectionHandler: RejectionHandler =
    RejectionHandler.newBuilder()
      .handle { case MissingQueryParamRejection(_) =>
        complete(BadRequest)
      }
      .handle { case AuthenticationFailedRejection(cause, _) =>
        cause match {
          case CredentialsMissing => complete((Unauthorized, MissingTokenError()))
          case CredentialsRejected => complete((Forbidden, InvalidTokenError()))
        }
      }
      .result()


  def authenticate(traceId: TraceId, authenticator: AuthFunction): Directive1[String] = {
    authenticateOAuth2Async(realm = "auction-house", authenticator)
  }

  def parseAuthHeader(authHeader: String): Option[String] = {
    val splitedHeader = authHeader.trim.split(" ", 2).map(_.trim)
    if (splitedHeader.size == 2) {
      Some(splitedHeader(1).trim)
    } else {
      None
    }
  }

  protected def requestsLatency: Histogram

  def authenticator(implicit traceId: TraceId): AuthFunction = {
    case Credentials.Provided(token) =>
      val histogramTimer = requestsLatency.labels("authenticate").startTimer()
      val result = validateToken(token)
      result.onComplete(_ => histogramTimer.observeDuration())
      result
    case _ => Future.successful(None)
  }
}
