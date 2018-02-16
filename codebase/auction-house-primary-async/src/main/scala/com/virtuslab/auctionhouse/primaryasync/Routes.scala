package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.virtuslab.identity.{CreateAccountRequest, SignInRequest, Token}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import scala.util.{Failure, Success}

trait Routes extends SprayJsonSupport with DefaultJsonProtocol {
  this: IdentityService =>

  private lazy val version = System.getProperty("service.version", "unknown")

  case class Status(version: String = version)
  case class Error(error: String)

  implicit lazy val carFormat: RootJsonFormat[CreateAccountRequest] = jsonFormat2(CreateAccountRequest)
  implicit lazy val sirFormat: RootJsonFormat[SignInRequest] = jsonFormat2(SignInRequest)
  implicit lazy val tokenFormat: RootJsonFormat[Token] = jsonFormat1(Token)

  implicit lazy val errorFormat: RootJsonFormat[Error] = jsonFormat1(Error)

  implicit lazy val statusFormat: RootJsonFormat[Status] = jsonFormat1(Status)

  lazy val routes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
        path("accounts") {
          post {
            entity(as[CreateAccountRequest]) { request =>
              onComplete(createUser(request)) {
                case Success(_) => complete(StatusCodes.Created)
                case Failure(d: DuplicateUser) => complete(StatusCodes.BadRequest, Error(s"user ${d.username} already exists"))
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
                case Failure(_: FailedSignIn) => complete(StatusCodes.BadRequest, Error("wrong password or username"))
                case Failure(exception) => failWith(exception)
              }
            }
          }
        }
      }
    } ~ path("_status") {
      complete(Status())
    }
}
