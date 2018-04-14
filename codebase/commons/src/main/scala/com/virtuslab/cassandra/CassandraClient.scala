package com.virtuslab.cassandra

import java.util.concurrent.Executors
import java.util.{Timer, TimerTask}

import com.datastax.driver.core.exceptions.{InvalidQueryException, NoHostAvailableException}
import com.datastax.driver.core.{Cluster, Session}
import com.virtuslab.Logging

import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.Try

trait CassandraClient {

  def getSession: Session

  def getSessionAsync: Future[Session]

}

trait CassandraClientImpl extends CassandraClient {
  this: Logging =>

  import com.virtuslab.AsyncUtils.Implicits._

  import scala.concurrent.duration._

  private val testQuery = "SELECT * FROM system_schema.keyspaces;"

  def cassandraContactPoint: String

  def connectWithRetries(retriesLeft: Int): Future[Session] = {
    import CassandraDriverHelpers._
    val sessionFutureOrError = for {
      session <- Future {
        Cluster.builder()
          .addContactPoint(cassandraContactPoint)
          .build()
          .connectAsync("microservices").asScala
      }.flatten
      _ <- session.executeAsync(testQuery).asScala
    } yield session

    sessionFutureOrError.recoverWith {
      case invalidQuery: InvalidQueryException if retriesLeft > 0 =>
        log.warn(s"Keyspace `microservices` doesn't seem to exist: ($invalidQuery), retrying in 5 sec. Retries " +
          s"left: $retriesLeft")
        waitFor(5000).flatMap(_ => connectWithRetries(retriesLeft - 1))
      case noHostAvailable: NoHostAvailableException if retriesLeft > 0 =>
        log.warn(s"Cassandra cluster seems to be down: ($noHostAvailable), retrying in 5 sec. Retries left: $retriesLeft")
        waitFor(5000).flatMap(_ => connectWithRetries(retriesLeft - 1))
    }
  }

  private val sessionFuture = connectWithRetries(60) // 60 * 5000

  override def getSession: Session = Await.result(sessionFuture, 15.seconds)
  override def getSessionAsync: Future[Session] = sessionFuture

}

object CassandraDriverHelpers {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor())

  def waitFor(delay: Long)(implicit ec: ExecutionContext): Future[Unit] = {
    val promise = Promise[Unit]()
    val t = new Timer()
    val tt: TimerTask = new TimerTask {
      override def run(): Unit = ec.execute(() => promise complete Try(()))
    }
    t.schedule(tt, delay)
    promise.future
  }

}