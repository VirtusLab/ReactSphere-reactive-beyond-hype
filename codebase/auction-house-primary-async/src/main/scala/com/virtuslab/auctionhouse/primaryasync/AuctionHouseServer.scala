package com.virtuslab.auctionhouse.primaryasync

import com.typesafe.scalalogging.Logger
import com.virtuslab.Config
import com.virtuslab.base.async.BaseServer

object AuctionHouseServer extends BaseServer with Routes {

  override protected val logger: Logger = Logger("AuctionHouse")

  override lazy val cassandraContactPoint: String = Config.cassandraContactPoint

}
