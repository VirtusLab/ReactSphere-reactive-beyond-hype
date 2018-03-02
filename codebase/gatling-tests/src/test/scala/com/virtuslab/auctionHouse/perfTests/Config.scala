package com.virtuslab.auctionHouse.perfTests

import com.typesafe.config.ConfigFactory

object Config {
  lazy val conf = ConfigFactory.load()
  lazy val apiVersion = conf.getString("api.version")
  lazy val serverHost = conf.getString("server.host")
  lazy val serverPort = conf.getString("server.port")
  lazy val serverHostPort = s"$serverHost:$serverPort"
}