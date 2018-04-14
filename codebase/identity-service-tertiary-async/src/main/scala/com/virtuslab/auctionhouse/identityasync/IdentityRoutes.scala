package com.virtuslab.auctionhouse.identityasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, Unauthorized}
import akka.http.scaladsl.server.Directives.{as, entity, failWith, onComplete, path, post, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.RoutingUtils
import com.virtuslab.identity._
import com.virtuslab.{CassandraQueriesMetrics, TraceId, TraceIdSupport}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

trait IdentityRoutes extends SprayJsonSupport with DefaultJsonProtocol
  with RoutingUtils with TraceIdSupport with CassandraQueriesMetrics {
  this: IdentityService =>

  implicit lazy val carFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat2(CreateAccountRequest)
  implicit lazy val sirFormat: RootJsonFormat[SignInRequest] = jsonFormat2(SignInRequest)
  implicit lazy val tokenFormat: RootJsonFormat[TokenResponse] = jsonFormat1(TokenResponse)
  implicit lazy val tvrFormat: RootJsonFormat[TokenValidationResponse] = jsonFormat1(TokenValidationResponse)
  implicit lazy val vtrFormat: RootJsonFormat[ValidateTokenRequest] = jsonFormat1(ValidateTokenRequest)

  protected def logger: Logger

  protected def requestsLatency: Histogram

  lazy val identityRoutes: Route =
    optionalHeaderValueByName("X-Trace-Id") { maybeTraceId =>
      implicit val traceId: TraceId = extractTraceId(maybeTraceId)
      path("accounts") {
        post {
          entity(as[CreateAccountRequest]) { request =>
            val histogramTimer = requestsLatency.labels("createAccountRequest").startTimer()
            logger.info(s"[${traceId.id}] Received account creation request for user '${request.username}' ...")

            onComplete(createUser(request)) {
              case Success(_) =>
                logger.info(s"[${traceId.id}] Account for '${request.username}' created.")
                histogramTimer.observeDuration()
                complete(Created)

              case Failure(d: DuplicateUser) =>
                logger.warn(s"[${traceId.id}] Account for '${request.username}' already exists.")
                histogramTimer.observeDuration()
                complete(BadRequest, Error(s"user '${d.username}' already exists"))

              case Failure(exception) =>
                logger.error(s"[${traceId.id}] Error occured while creating account for user '${request.username}':", exception)
                histogramTimer.observeDuration()
                failWith(exception)
            }
          }
        }
      } ~
        path("sign-in") {
          post {
            entity(as[SignInRequest]) { request =>
              val histogramTimer = requestsLatency.labels("signInRequest").startTimer()
              logger.info(s"[${traceId.id}] Received sign in request for user '${request.username}' ...")

              onComplete(signIn(request)) {
                case Success(token) =>
                  logger.info(s"[${traceId.id}] Successful sign in for user '${request.username}', responding with access token.")
                  histogramTimer.observeDuration()
                  complete(TokenResponse(token))

                case Failure(_: FailedSignIn) =>
                  logger.warn(s"[${traceId.id}] Authentication failure for '${request.username}' detected.")
                  histogramTimer.observeDuration()
                  complete(Unauthorized, Error("wrong password or username"))

                case Failure(exception) =>
                  logger.error(s"[${traceId.id}] Error occured while signing in '${request.username}':", exception)
                  histogramTimer.observeDuration()
                  failWith(exception)
              }
            }
          }
        } ~
        path("validate") {
          post {
            entity(as[ValidateTokenRequest]) { request =>
              val histogramTimer = requestsLatency.labels("validateTokenRequest").startTimer()
              logger.info(s"[${traceId.id}] Received validate token request...")

              onComplete(validateToken(request.token)) {
                case Success(Some(username)) =>
                  logger.info(s"[${traceId.id}] Successful validation of token, responding with user name '$username'.")
                  histogramTimer.observeDuration()
                  complete(TokenValidationResponse(username))
                case Success(None) =>
                  logger.warn(s"[${traceId.id}] Token validation failure, responding with 401 Unauthorized.")
                  histogramTimer.observeDuration()
                  complete(Unauthorized)
                case Failure(exception) =>
                  logger.error(s"[${traceId.id}] Error occured while validating token:", exception)
                  histogramTimer.observeDuration()
                  failWith(exception)
              }
            }
          }
        }
    }
}
