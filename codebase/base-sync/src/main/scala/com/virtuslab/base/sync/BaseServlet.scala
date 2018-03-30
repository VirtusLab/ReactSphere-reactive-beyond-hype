package com.virtuslab.base.sync

import com.typesafe.scalalogging.Logger
import com.virtuslab.{Logging, RequestMetrics, TraceId, TraceIdSupport}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport

trait BaseServlet extends ScalatraServlet with JacksonJsonSupport with RequestMetrics with TraceIdSupport with Logging {

  def servletName: String

  override protected val log: Logger = Logger(servletName)

  override protected implicit def jsonFormats: Formats = DefaultFormats

  def getTraceId: TraceId = extractTraceId(request.header("X-Trace-Id"))

  error {
    case e: Throwable => {
      log.error("Unexpected error", e)
      throw e
    }
  }

}
