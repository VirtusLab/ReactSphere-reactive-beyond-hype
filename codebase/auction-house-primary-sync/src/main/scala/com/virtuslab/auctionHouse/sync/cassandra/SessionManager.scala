package com.virtuslab.auctionHouse.sync.cassandra

import com.datastax.driver.core.Cluster
import com.datastax.driver.mapping.{Mapper, MappingManager}
import com.virtuslab.auctionHouse.sync.Config

trait SessionManager {

  protected lazy val cluster = Cluster.builder()
    .addContactPoint(Config.cassandraContactPoint)
    .build()

  val keyspace = "auction_house"
  lazy val session = cluster.connect(keyspace)

  protected lazy val mappingManager = new MappingManager(session)

  def mapper[T](clazz: Class[T]) = mappingManager.mapper(clazz)
  def mapper[T](clazz: Class[T], keyspace: String) = mappingManager.mapper(clazz, keyspace)
}

object SessionManager extends SessionManager {
  implicit class ScalaMapper[T](mapper: Mapper[T]) {
    def getOption[R](objects: AnyRef*): Option[T] = Option(mapper.get(objects: _*))
  }
}
