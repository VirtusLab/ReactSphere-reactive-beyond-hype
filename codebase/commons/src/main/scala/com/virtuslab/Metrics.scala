package com.virtuslab

import io.prometheus.client.Histogram

object Metrics {

  val requestsLatencyHistogram: Histogram = Histogram.build()
    .name("requests_latency_seconds")
    .help("Request latency in seconds.")
    .labelNames("requestType")
    .register()

}
