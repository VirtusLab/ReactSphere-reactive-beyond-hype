package com.virtuslab

import io.prometheus.client.{Gauge, Histogram}

object Metrics {

  val cassandraQueryLatencies: Histogram = Histogram.build()
    .name("cassandra_query_latencies")
    .help("Cassandra query latency in seconds.")
    .labelNames("queryType")
    .register()

  val requestsLatencyHistogram: Histogram = Histogram.build()
    .name("requests_latency_seconds")
    .help("Request latency in seconds.")
    .labelNames("requestType")
    .register()

  val cassandraQueriesGauge: Gauge = Gauge.build()
    .name("pending_cassandra_queries")
    .help("Cassandra queries currently under execution.")
    .register()

}
