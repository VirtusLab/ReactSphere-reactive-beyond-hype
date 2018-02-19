package com.virtuslab.auctionhouse.primaryasync

import com.datastax.driver.core.Session

import scala.concurrent.Future

trait CassandraClient {

  def getSession: Session
  def getSessionAsync: Future[Session]

}

trait CassandraClientImpl extends CassandraClient {
  override def getSession: Session = ???
  override def getSessionAsync: Future[Session] = ???
}