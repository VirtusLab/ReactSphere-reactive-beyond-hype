package com.virtuslab.billings.async

import com.typesafe.scalalogging.Logger
import com.virtuslab.Config
import com.virtuslab.base.async.BaseServer

object BillingServer extends BaseServer(defaultPort = 8090) with Routes {

  override protected val logger: Logger = Logger("BillingService")

  override def main(args: Array[String]): Unit = {
    Config.logAwsKeys(logger)
    super.main(args)
  }
}