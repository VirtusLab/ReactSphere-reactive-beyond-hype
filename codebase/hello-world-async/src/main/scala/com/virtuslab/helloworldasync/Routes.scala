package com.virtuslab.helloworldasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import spray.json.RootJsonFormat

trait Routes extends SprayJsonSupport {

  private lazy val version = System.getProperty("service.version", "unknown")

  case class HelloWorld(message: String = "Hello World!")
  case class Status(version: String = version)

  import spray.json.DefaultJsonProtocol._

  implicit lazy val helloWorldFormat: RootJsonFormat[HelloWorld] = jsonFormat1(HelloWorld)
  implicit lazy val statusFormat: RootJsonFormat[Status] = jsonFormat1(Status)

  lazy val routes: Route =
    pathEndOrSingleSlash {
      get {
        complete(HelloWorld())
      }
    } ~ path("_status") {
      complete(Status())
    }
}
