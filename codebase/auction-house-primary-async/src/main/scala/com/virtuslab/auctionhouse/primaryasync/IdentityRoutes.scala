package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes.{BadRequest, Created, Unauthorized}
import akka.http.scaladsl.server.Directives.{as, entity, failWith, onComplete, path, pathPrefix, post, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest, Token}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

trait IdentityRoutes extends SprayJsonSupport with DefaultJsonProtocol with RoutingUtils {
  this: IdentityService =>

  implicit lazy val carFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat2(CreateAccountRequest)
  implicit lazy val sirFormat: RootJsonFormat[SignInRequest] = jsonFormat2(SignInRequest)
  implicit lazy val tokenFormat: RootJsonFormat[Token] = jsonFormat1(Token)

  lazy val identityRoutes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
        path("accounts") {
          post {
            entity(as[CreateAccountRequest]) { request =>
              onComplete(createUser(request)) {
                case Success(_) => complete(Created)
                case Failure(d: DuplicateUser) => complete(BadRequest, Error(s"user ${d.username} already exists"))
                case Failure(exception) => failWith(exception)
              }
            }
          }
        } ~
          path("sign-in") {
            post {
              entity(as[SignInRequest]) { request =>
                onComplete(signIn(request)) {
                  case Success(token) => complete(Token(token))
                  case Failure(_: FailedSignIn) => complete(Unauthorized, Error("wrong password or username"))
                  case Failure(exception) => failWith(exception)
                }
              }
            }
          }
      }
    }

}
