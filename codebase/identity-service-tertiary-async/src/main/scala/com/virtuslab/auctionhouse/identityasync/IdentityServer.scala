package com.virtuslab.auctionhouse.identityasync

import com.typesafe.scalalogging.Logger
import com.virtuslab.BaseConfig
import com.virtuslab.base.async.BaseServer

object IdentityServer extends BaseServer(defaultPort = 8100) with Routes {

  override protected val logger: Logger = Logger("IdentityService")

  override lazy val cassandraContactPoint: String = BaseConfig.cassandraContactPoint

}
