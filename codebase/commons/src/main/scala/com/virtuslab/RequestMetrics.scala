package com.virtuslab

import io.prometheus.client.Histogram

trait RequestMetrics { this: Logging =>

  protected lazy val requestsLatency: Histogram = Metrics.requestsLatencyHistogram

  protected def timing[T](label: String)(f: => T): T = {
    val timer = requestsLatency.labels(label).startTimer()
    val result = f
    timer.observeDuration()

    result
  }
}
