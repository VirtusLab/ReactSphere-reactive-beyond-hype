package com.virtuslab

import com.typesafe.config.{ConfigFactory, Config => TypesafeConfig}
import com.typesafe.scalalogging.Logger

object Config {
  lazy val conf: TypesafeConfig = ConfigFactory.load()

  lazy val cassandraContactPoint: String = conf.getString("cassandra.contactPoint")
  lazy val identityServiceContactPoint: String = conf.getString("identityService.contactPoint")
  lazy val billingServiceContactPoint: String = conf.getString("billingService.contactPoint")
  lazy val paymentSystemContactPoint: String = conf.getString("paymentSystem.contactPoint")

  lazy val httpResponseTimeout: Int = conf.getInt("http.responseTimeout")
  lazy val httpConnectionTimeout: Int = conf.getInt("http.connectionTimeout")

  def logAwsKeys(log: Logger, obfuscate: Boolean = true): Unit = {
    val key = Option(System.getenv("AWS_ACCESS_KEY_ID"))
    val secret = Option(System.getenv("AWS_SECRET_ACCESS_KEY"))

    val awsVals = Seq(key, secret).map {
      case Some(value) if obfuscate =>
        val prefixLength = 5
        val suffixLength = value.length - prefixLength
        value.substring(0, prefixLength) + 0.until(suffixLength).map(_ => "*").mkString
      case Some(value) =>
        value
      case None =>
        "<NOT SET>"
    }

    log.info(s"Read AWS credentials are: ${awsVals.mkString(" / " )}")
  }
}