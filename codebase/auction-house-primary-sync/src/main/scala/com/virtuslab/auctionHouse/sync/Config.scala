package com.virtuslab.auctionHouse.sync

import com.typesafe.config.ConfigFactory

object Config {
  lazy val conf = ConfigFactory.load()
  lazy val cassandraContactPoint = conf.getString("cassandra.contactPoint")
}
