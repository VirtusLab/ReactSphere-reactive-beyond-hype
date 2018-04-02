package com.virtuslab.auctionhouse.identityasync

import com.typesafe.scalalogging.Logger
import com.virtuslab.Config
import com.virtuslab.base.async.BaseServer

object IdentityServer extends BaseServer(defaultPort = 8100) with Routes {

  override protected val logger: Logger = Logger("IdentityService")

  override lazy val cassandraContactPoint: String = Config.cassandraContactPoint

}
