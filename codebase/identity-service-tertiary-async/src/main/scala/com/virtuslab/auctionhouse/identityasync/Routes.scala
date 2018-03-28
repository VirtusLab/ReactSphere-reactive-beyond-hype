package com.virtuslab.auctionhouse.identityasync

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.RequestMetrics
import com.virtuslab.base.async.{BaseRoutes, RoutingUtils}
import com.virtuslab.cassandra.CassandraClientImpl

trait Routes extends BaseRoutes
  with IdentityRoutes with IdentityServiceImpl
  with RoutingUtils with CassandraClientImpl
  with RequestMetrics {

  protected def logger: Logger

  def serviceRoutes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
        identityRoutes
      }
    }

}
