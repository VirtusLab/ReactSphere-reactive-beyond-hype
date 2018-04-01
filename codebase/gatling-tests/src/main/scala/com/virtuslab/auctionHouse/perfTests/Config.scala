package com.virtuslab.auctionHouse.perfTests

import com.typesafe.config.{Config, ConfigFactory}

object Config {

  lazy val conf: Config = ConfigFactory.load()
  lazy val apiVersion: String = conf.getString("api.version")
  lazy val useS3: Boolean = conf.getBoolean("reports.useS3")
  lazy val identityServiceHostPort = s"${conf.getString("identityService.host")}:${conf.getString("identityService.port")}"
  lazy val auctionServiceHostPort = s"${conf.getString("auctionService.host")}:${conf.getString("auctionService.port")}"
}