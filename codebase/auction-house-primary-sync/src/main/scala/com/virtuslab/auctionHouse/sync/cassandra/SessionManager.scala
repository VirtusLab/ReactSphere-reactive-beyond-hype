package com.virtuslab.auctionHouse.sync.cassandra

import com.datastax.driver.core.{Cluster, Session}
import com.datastax.driver.mapping.{Mapper, MappingManager}
import com.virtuslab.Config

trait SessionManager {

  protected lazy val cluster: Cluster =
    Cluster.builder()
      .addContactPoint(Config.cassandraContactPoint)
      .build()

  val keyspace = "microservices"

  lazy val session: Session = cluster.connect(keyspace)

  protected lazy val mappingManager = new MappingManager(session)

  def mapper[T](clazz: Class[T]): Mapper[T] = mappingManager.mapper(clazz)
  def mapper[T](clazz: Class[T], keyspace: String): Mapper[T] = mappingManager.mapper(clazz, keyspace)
}

object SessionManager extends SessionManager {
  implicit class ScalaMapper[T](mapper: Mapper[T]) {
    def getOption[R](objects: AnyRef*): Option[T] = Option(mapper.get(objects: _*))
  }
}
