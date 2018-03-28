package com.virtuslab.base.async

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.typesafe.scalalogging.Logger
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait BaseRoutes extends SprayJsonSupport with DefaultJsonProtocol {

  def serviceRoutes: Route

  protected def logger: Logger

  lazy val routes: Route =
    path("_status") {
      logger.info("Responding to status request.")
      complete(Status())
    } ~ serviceRoutes

  private lazy val version = System.getProperty("service.version", "unknown")

  implicit lazy val statusFormat: RootJsonFormat[Status] = jsonFormat1(Status)

  case class Status(version: String = version)

}
