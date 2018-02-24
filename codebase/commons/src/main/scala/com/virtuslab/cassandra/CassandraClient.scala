package com.virtuslab.cassandra

import com.datastax.driver.core.{Cluster, Session}

import scala.concurrent.{Await, Future}

trait CassandraClient {

  def getSession: Session
  def getSessionAsync: Future[Session]

}

trait CassandraClientImpl extends CassandraClient {

  import com.virtuslab.AsyncUtils.Implicits._
  import scala.concurrent.duration._

  def cassandraContactPoint: String

  private lazy val cluster = Cluster.builder().addContactPoint(cassandraContactPoint).build()
  private lazy val sessionFuture = cluster.connectAsync("microservices").asScala

  override def getSession: Session = Await.result(sessionFuture, 15.seconds)
  override def getSessionAsync: Future[Session] = sessionFuture

}