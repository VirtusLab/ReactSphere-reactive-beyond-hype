package com.virtuslab.billings.async

import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.BaseServer

object BillingServer extends BaseServer with Routes {

  override protected val logger: Logger = Logger("BillingService")

}