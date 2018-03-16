package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, Unauthorized}
import akka.http.scaladsl.server.Directives.{as, entity, failWith, onComplete, path, post, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.typesafe.scalalogging.Logger
import com.virtuslab.{TraceId, TraceIdSupport}
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest, TokenResponse}
import io.prometheus.client.Histogram
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

trait IdentityRoutes extends SprayJsonSupport with DefaultJsonProtocol
  with RoutingUtils with TraceIdSupport {
  this: IdentityService =>

  implicit lazy val carFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat2(CreateAccountRequest)
  implicit lazy val sirFormat: RootJsonFormat[SignInRequest] = jsonFormat2(SignInRequest)
  implicit lazy val tokenFormat: RootJsonFormat[TokenResponse] = jsonFormat1(TokenResponse)

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
        }
    }
}
