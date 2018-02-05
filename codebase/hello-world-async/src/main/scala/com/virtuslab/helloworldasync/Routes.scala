package com.virtuslab.helloworldasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.PathDirectives.pathEndOrSingleSlash
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import spray.json.RootJsonFormat

trait Routes extends SprayJsonSupport {

  case class HelloWorld(message: String = "Hello World!")

  import spray.json.DefaultJsonProtocol._

  implicit lazy val helloWorldFormat: RootJsonFormat[HelloWorld] = jsonFormat1(HelloWorld)

  lazy val routes: Route =
    pathEndOrSingleSlash {
      get {
        complete(HelloWorld())
      }
    }
}
