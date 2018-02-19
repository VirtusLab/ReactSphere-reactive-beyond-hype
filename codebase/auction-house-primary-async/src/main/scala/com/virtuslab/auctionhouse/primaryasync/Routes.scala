package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.virtuslab.cassandra.CassandraClientImpl
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

trait Routes extends SprayJsonSupport with DefaultJsonProtocol with IdentityRoutes with IdentityServiceImpl
  with RoutingUtils with CassandraClientImpl {

  lazy val routes: Route =
    identityRoutes ~
      path("_status") {
        complete(Status())
      }

  private lazy val version = System.getProperty("service.version", "unknown")

  implicit lazy val statusFormat: RootJsonFormat[Status] = jsonFormat1(Status)

  case class Status(version: String = version)
}
