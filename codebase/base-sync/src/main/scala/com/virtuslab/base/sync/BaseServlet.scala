package com.virtuslab.base.sync

import com.typesafe.scalalogging.Logger
import com.virtuslab.{RequestMetrics, TraceId, TraceIdSupport}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

trait BaseServlet extends ScalatraServlet with JacksonJsonSupport with RequestMetrics with TraceIdSupport {

  def servletName: String

  protected val logger: Logger = Logger(servletName)

  override protected implicit def jsonFormats: Formats = DefaultFormats

  def getTraceId: TraceId = extractTraceId(request.header("X-Trace-Id"))

  error {
    case e: Throwable => {
      logger.error("Unexpected error", e)
      throw e
    }
  }

}
