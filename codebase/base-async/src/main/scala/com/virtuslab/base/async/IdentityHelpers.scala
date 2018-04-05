package com.virtuslab.base.async

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.stream.Materializer
import akka.util.ByteString
import com.virtuslab.identity.{TokenValidationResponse, ValidateTokenRequest}
import com.virtuslab.{Config, TraceId}
import spray.json.DefaultJsonProtocol

import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

trait IdentityHelpers extends DefaultJsonProtocol {

  import spray.json._

  private implicit lazy val tvrFormat: RootJsonFormat[TokenValidationResponse] = jsonFormat1(TokenValidationResponse)
  private implicit lazy val vtrFormat: RootJsonFormat[ValidateTokenRequest] = jsonFormat1(ValidateTokenRequest)

  protected implicit def materializer: Materializer
  protected implicit def system: ActorSystem

  protected implicit def executionContext: ExecutionContext

  private val identityUrl = s"http://${Config.identityServiceContactPoint}/api/v1/validate"

  def validateToken(token: String)(implicit traceId: TraceId): Future[Option[String]] = {
    val json = ValidateTokenRequest(token).toJson.compactPrint
    val entity = HttpEntity(`application/json`, json)
    val request = HttpRequest(POST, identityUrl)
      .withHeaders(RawHeader("X-Trace-Id", traceId.id))
      .withEntity(entity)

    Http().singleRequest(request).flatMap { response =>
      if (response.status.isSuccess())
        response.entity
          .dataBytes
          .runFold(ByteString(""))(_ ++ _)
          .map { bs =>
            Some {
              bs.utf8String
                .parseJson.convertTo[TokenValidationResponse]
                .username
            }
          }
      else
        successful(None)
    }
  }

  case class MissingTokenError(error: String = "authentication token is missing")

  case class InvalidTokenError(error: String = "authentication token is invalid")

}
