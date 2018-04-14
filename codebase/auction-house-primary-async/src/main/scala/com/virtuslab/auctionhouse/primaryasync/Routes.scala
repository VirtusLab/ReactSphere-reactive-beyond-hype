package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.{CassandraQueriesMetrics, RequestMetrics, TraceIdSupport}
import com.virtuslab.base.async.{BaseRoutes, IdentityHelpers, RoutingUtils}
import com.virtuslab.cassandra.CassandraClientImpl

trait Routes extends BaseRoutes
  with AuctionRoutes with AuctionServiceImpl with IdentityHelpers
  with RoutingUtils with CassandraClientImpl
  with RequestMetrics with CassandraQueriesMetrics
  with TraceIdSupport {

  protected def logger: Logger

  override def serviceRoutes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
          auctionRoutes
      }
    }

}
