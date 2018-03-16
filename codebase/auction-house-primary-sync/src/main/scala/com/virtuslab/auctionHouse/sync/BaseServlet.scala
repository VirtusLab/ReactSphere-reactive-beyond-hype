package com.virtuslab.auctionHouse.sync

import com.typesafe.scalalogging.Logger
import com.virtuslab.{RequestMetrics, TraceId, TraceIdSupport}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

trait BaseServlet extends ScalatraServlet with JacksonJsonSupport with RequestMetrics with TraceIdSupport {

  protected val logger: Logger = Logger("AuctionHouse")

  override protected implicit def jsonFormats: Formats = DefaultFormats

  def getTraceId: TraceId = extractTraceId(request.header("X-Trace-Id"))

  error {
    case e: Throwable => {
      logger.error("Unexpected error", e)
      throw e
    }
  }

}
