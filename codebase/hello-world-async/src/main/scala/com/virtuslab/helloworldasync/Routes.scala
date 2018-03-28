package com.virtuslab.helloworldasync

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.virtuslab.base.async.BaseRoutes
import spray.json.RootJsonFormat

trait Routes extends BaseRoutes {

  case class HelloWorld(message: String = "Hello World!")

  implicit lazy val helloWorldFormat: RootJsonFormat[HelloWorld] = jsonFormat1(HelloWorld)

  lazy val serviceRoutes: Route =
    pathEndOrSingleSlash {
      get {
        complete(HelloWorld())
      }
    }
}
