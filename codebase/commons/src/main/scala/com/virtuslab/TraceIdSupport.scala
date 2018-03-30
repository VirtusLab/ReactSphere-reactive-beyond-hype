package com.virtuslab

import java.util.UUID

trait TraceIdSupport {

  type Headers = Seq[(String, String)]

  def traceHeaders(implicit traceId: TraceId): Headers =
    ("X-Trace-Id", traceId.id) :: ("Content-Type", "application/json") :: Nil

  def generateTraceIt: TraceId = TraceId(UUID.randomUUID().toString)

  def extractTraceId(maybeTraceId: Option[String]): TraceId =
    maybeTraceId map (id => TraceId(id)) getOrElse generateTraceIt

}
