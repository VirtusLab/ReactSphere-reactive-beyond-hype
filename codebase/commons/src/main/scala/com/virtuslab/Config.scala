package com.virtuslab

import com.typesafe.config.{Config, ConfigFactory}

object Config {
  lazy val conf: Config = ConfigFactory.load()
  lazy val cassandraContactPoint: String = conf.getString("cassandra.contactPoint")
}
