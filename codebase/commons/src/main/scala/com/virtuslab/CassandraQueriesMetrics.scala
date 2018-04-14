package com.virtuslab

import io.prometheus.client.{Gauge, Histogram}

trait CassandraQueriesMetrics {

  protected lazy val cassandraQueries: Gauge = Metrics.cassandraQueriesGauge

  protected lazy val cassandraQueryLatencies: Histogram = Metrics.cassandraQueryLatencies

  protected def usingCassandra[T](count: Int)(f: => T): T = {
    cassandraQueries.inc(count)
    try f finally cassandraQueries.dec(count)
  }

}
