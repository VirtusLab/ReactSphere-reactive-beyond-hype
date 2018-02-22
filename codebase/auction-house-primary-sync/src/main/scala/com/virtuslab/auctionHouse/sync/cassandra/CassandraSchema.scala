package com.virtuslab.auctionHouse.sync.cassandra

import org.slf4j.LoggerFactory

object CassandraSchema {
  val log = LoggerFactory.getLogger(getClass)

  val cmds = Seq(
    "CREATE KEYSPACE IF NOT EXISTS auction_house  WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 1 }",
    """CREATE TABLE  auction_house.accounts (
      |username varchar PRIMARY KEY,
      |password text,
      |)
    """.stripMargin,
    """CREATE TABLE  auction_house.tokens (
      |"token" varchar PRIMARY KEY,
      |username varchar,
      |expires_at timestamp
      |)""".stripMargin
  )

  def generateSchema: Unit = {
    cmds.foreach { cmd =>
      log.info(s"Executing cmd:\n$cmd")
      SessionManager.session.execute(cmd)
    }
  }

  def recreateSchema: Unit = {
    SessionManager.session.execute("DROP KEYSPACE auction_house")
    generateSchema
  }
}
