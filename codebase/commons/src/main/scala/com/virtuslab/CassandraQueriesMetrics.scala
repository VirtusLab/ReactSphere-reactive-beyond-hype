package com.virtuslab

import io.prometheus.client.{Gauge, Histogram}

import scala.concurrent.{ExecutionContext, Future}

trait CassandraQueriesMetrics {
  this: Logging =>

  protected val cassandraQuery: Gauge = Metrics.cassandraQueriesGauge

  private val cassandraQueryLatencies: Histogram = Metrics.cassandraQueryLatencies

  protected def cassandraTimingSync[T](count: Int, label: String)(f: => T): T = {
    val timer = cassandraQueryLatencies.labels(label).startTimer()
    cassandraQuery.inc(count)

    try {
      f
    } finally {
      cassandraQuery.dec(count)
      val time = timer.observeDuration()
      log.info(s"Query [${label}] took ${time}")
    }
  }

  protected def cassandraTimingAsync[T](count: Int, label: String)(block: => Future[T])
                                       (implicit ec: ExecutionContext): Future[T] = {
    val timer = cassandraQueryLatencies.labels(label).startTimer()
    cassandraQuery.inc(count)

    val futureExecution = block
    futureExecution.onComplete { _ =>
      cassandraQuery.inc(count)
      val time = timer.observeDuration()
      log.info(s"Query [${label}] took ${time}")
    }

    futureExecution
  }

}
