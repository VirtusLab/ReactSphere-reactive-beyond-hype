package com.virtuslab.auctionHouse.perfTests

import com.typesafe.config.{Config, ConfigFactory}

object Config {

  lazy val conf: Config = ConfigFactory.load()
  lazy val apiVersion: String = conf.getString("api.version")
  lazy val serverHost: String = conf.getString("server.host")
  lazy val serverPort: String = conf.getString("server.port")
  lazy val useS3: Boolean = conf.getBoolean("reports.useS3")
  lazy val serverHostPort = s"$serverHost:$serverPort"

}