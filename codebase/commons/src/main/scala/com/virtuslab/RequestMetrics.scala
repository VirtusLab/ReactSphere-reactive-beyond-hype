package com.virtuslab

import io.prometheus.client.Histogram

trait RequestMetrics {

  protected lazy val requestsLatency: Histogram = Metrics.requestsLatencyHistogram

}
