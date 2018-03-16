package com.virtuslab

import java.util.UUID

trait TraceIdSupport {

  def generateTraceIt: TraceId = TraceId(UUID.randomUUID().toString)

  def extractTraceId(maybeTraceId: Option[String]): TraceId =
    maybeTraceId map (id => TraceId(id)) getOrElse generateTraceIt

}
