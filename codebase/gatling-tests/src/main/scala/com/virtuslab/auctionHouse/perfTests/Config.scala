package com.virtuslab.auctionHouse.perfTests

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.{Duration, DurationInt}

object Config {

  lazy val conf: Config = ConfigFactory.load()
  lazy val apiVersion: String = conf.getString("api.version")
  lazy val useS3: Boolean = conf.getBoolean("reports.useS3")

  lazy val startDelay: Duration =  conf.getInt("startDelaySeconds").seconds

  lazy val auctionServiceContactPoint = s"${conf.getString("auctionService.contactPoint")}"
  lazy val identityServiceContactPoint = s"${conf.getString("identityService.contactPoint")}"
}