package com.virtuslab.auctionhouse.identityasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, Unauthorized}
import akka.http.scaladsl.server.Directives.{as, entity, failWith, onComplete, path, post, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.RoutingUtils
import com.virtuslab.identity._
import com.virtuslab.{CassandraQueriesMetrics, Logging, TraceId, TraceIdSupport}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

trait IdentityRoutes extends SprayJsonSupport with DefaultJsonProtocol
  with RoutingUtils with TraceIdSupport with CassandraQueriesMetrics with Logging {
  this: IdentityService =>

  implicit lazy val carFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat2(CreateAccountRequest)
  implicit lazy val sirFormat: RootJsonFormat[SignInRequest] = jsonFormat2(SignInRequest)
  implicit lazy val tokenFormat: RootJsonFormat[TokenResponse] = jsonFormat1(TokenResponse)
  implicit lazy val tvrFormat: RootJsonFormat[TokenValidationResponse] = jsonFormat1(TokenValidationResponse)
  implicit lazy val vtrFormat: RootJsonFormat[ValidateTokenRequest] = jsonFormat1(ValidateTokenRequest)

  protected val log: Logger = Logger(getClass)

  lazy val identityRoutes: Route =
    optionalHeaderValueByName("X-Trace-Id") { maybeTraceId =>
      implicit val traceId: TraceId = extractTraceId(maybeTraceId)
      path("accounts") {
        post {
          entity(as[CreateAccountRequest]) { request =>
            log.info(s"[${traceId.id}] Received account creation request for user '${request.username}' ...")

            onComplete(createUser(request)) {
              case Success(_) =>
                log.info(s"[${traceId.id}] Account for '${request.username}' created.")
                complete(Created)

              case Failure(d: DuplicateUser) =>
                log.warn(s"[${traceId.id}] Account for '${request.username}' already exists.")
                complete(BadRequest, Error(s"user '${d.username}' already exists"))

              case Failure(exception) =>
                log.error(s"[${traceId.id}] Error occured while creating account for user '${request.username}':", exception)
                failWith(exception)
            }
          }
        }
      } ~
        path("sign-in") {
          post {
            entity(as[SignInRequest]) { request =>
              log.info(s"[${traceId.id}] Received sign in request for user '${request.username}' ...")

              onComplete(signIn(request)) {
                case Success(token) =>
                  log.info(s"[${traceId.id}] Successful sign in for user '${request.username}', responding with access token.")
                  complete(TokenResponse(token))

                case Failure(_: FailedSignIn) =>
                  log.warn(s"[${traceId.id}] Authentication failure for '${request.username}' detected.")
                  complete(Unauthorized, Error("wrong password or username"))

                case Failure(exception) =>
                  log.error(s"[${traceId.id}] Error occured while signing in '${request.username}':", exception)
                  failWith(exception)
              }
            }
          }
        } ~
        path("validate") {
          post {
            entity(as[ValidateTokenRequest]) { request =>
              log.info(s"[${traceId.id}] Received validate token request...")

              onComplete(validateToken(request.token)) {
                case Success(Some(username)) =>
                  log.info(s"[${traceId.id}] Successful validation of token, responding with user name '$username'.")
                  complete(TokenValidationResponse(username))
                case Success(None) =>
                  log.warn(s"[${traceId.id}] Token validation failure, responding with 401 Unauthorized.")
                  complete(Unauthorized)
                case Failure(exception) =>
                  log.error(s"[${traceId.id}] Error occured while validating token:", exception)
                  failWith(exception)
              }
            }
          }
        }
    }
}
