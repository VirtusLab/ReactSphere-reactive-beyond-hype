package com.virtuslab

import com.typesafe.config.{Config => TypesafeConfig, ConfigFactory}

object Config {
  lazy val conf: TypesafeConfig = ConfigFactory.load()

  lazy val cassandraContactPoint: String = conf.getString("cassandra.contactPoint")
  lazy val identityServiceContactPoint: String = conf.getString("identityService.contactPoint")
  lazy val billingServiceContactPoint: String = conf.getString("billingService.contactPoint")
  lazy val paymentSystemContactPoint: String = conf.getString("paymentSystem.contactPoint")

  lazy val httpResponseTimeout: Int = conf.getInt("http.responseTimeout")
  lazy val httpConnectionTimeout: Int = conf.getInt("http.connectionTimeout")
}