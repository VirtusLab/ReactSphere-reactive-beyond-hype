package com.virtuslab.base.async

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.virtuslab.Logging
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait BaseRoutes extends SprayJsonSupport with DefaultJsonProtocol {
  this: Logging =>

  def serviceRoutes: Route

  lazy val routes: Route =
    path("_status") {
      log.info("Responding to status request.")
      complete(Status())
    } ~ serviceRoutes

  private lazy val version = System.getProperty("service.version", "unknown")

  implicit lazy val statusFormat: RootJsonFormat[Status] = jsonFormat1(Status)

  case class Status(version: String = version)

}
