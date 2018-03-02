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
    SessionManager.session.execute(cmds.head)
    all.split(";").map(_.trim).filterNot(_.isEmpty).foreach { cmd =>
            println(s"Executing cmd:\n$cmd")
            SessionManager.session.execute(cmd)
          }
  }

  def recreateSchema: Unit = {
    SessionManager.session.execute("DROP KEYSPACE auction_house")
    generateSchema
  }

  val all =
    """
      |CREATE TABLE auction_house.accounts (
      |    username text,
      |    password text,
      |    PRIMARY KEY (username)
      |);
      |
      |CREATE TABLE auction_house.tokens (
      |    bearer_token text,
      |    username text,
      |    expires_at timestamp,
      |    PRIMARY KEY (bearer_token)
      |);
      |
      |CREATE TABLE auction_house.auctions (
      |    category text,
      |    created_at timestamp,
      |    auction_id uuid,
      |    owner text,
      |    title text,
      |    description text,
      |    details text,
      |    minimum_price decimal,
      |    PRIMARY KEY (category, created_at, auction_id)
      |) WITH CLUSTERING ORDER BY (created_at DESC);
      |
      |CREATE MATERIALIZED VIEW auction_house.auctions_view AS
      |    SELECT * FROM auction_house.auctions
      |    WHERE auction_id IS NOT NULL AND created_at IS NOT NULL AND category IS NOT NULL
      |    PRIMARY KEY (auction_id, created_at, category);
      |
      |CREATE TABLE auction_house.bids (
      |    auction_id uuid,
      |    bid_id timeuuid,
      |    bidder text,
      |    amount decimal,
      |    PRIMARY KEY (auction_id, bid_id)
      |);
    """.stripMargin
}