package com.virtuslab.billings.async

import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.{BaseRoutes, IdentityHelpers, RoutingUtils}
import com.virtuslab.{RequestMetrics, TraceIdSupport}

trait Routes extends BaseRoutes with BillingRoutes
   with IdentityHelpers
  with RoutingUtils
  with RequestMetrics with TraceIdSupport {

  protected def logger: Logger

  override def serviceRoutes: Route =
    pathPrefix("api") {
      pathPrefix("v1") {
        billingRoutes
      }
    }

}