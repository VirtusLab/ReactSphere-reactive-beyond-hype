package com.virtuslab.auctionhouse.primaryasync

import com.typesafe.scalalogging.Logger
import com.virtuslab.Config
import com.virtuslab.base.async.BaseServer

object AuctionHouseServer extends BaseServer(defaultPort = 8080) with Routes {

  override protected val log: Logger = Logger("AuctionHouse")

  override lazy val cassandraContactPoint: String = Config.cassandraContactPoint

}
