package com.virtuslab.auctionhouse.primaryasync

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.{BaseRoutes, IdentityHelpers, RoutingUtils}
import com.virtuslab.cassandra.CassandraClientImpl
import com.virtuslab.{CassandraQueriesMetrics, Logging, RequestMetrics, TraceIdSupport}

trait Routes extends BaseRoutes
  with AuctionRoutes with AuctionServiceImpl with IdentityHelpers
  with RoutingUtils with CassandraClientImpl
  with RequestMetrics with CassandraQueriesMetrics with TraceIdSupport with Logging {

  override protected val log: Logger

  override def serviceRoutes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
          auctionRoutes
      }
    }

}
