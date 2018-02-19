package com.virtuslab.auctionHouse.sync.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.mapping.MappingManager
import com.virtuslab.auctionHouse.sync.Config

trait CassandraSession {

  lazy val cluster = Cluster.builder()
    .addContactPoint(Config.cassandraContactPoint)
    .build()

  lazy val session = cluster.connect()

  lazy val mappingManager = new MappingManager(session)
}

object CassandraSession extends CassandraSession
