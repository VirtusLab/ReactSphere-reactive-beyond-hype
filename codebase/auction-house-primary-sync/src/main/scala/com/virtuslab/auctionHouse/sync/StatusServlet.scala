package com.virtuslab.auctionHouse.sync

class StatusServlet extends BaseServlet {

  private lazy val version = System.getProperty("service.version", "unknown")

  case class Status(version: String = version)

  before() {
    contentType = formats("json")
  }

  get("/_status") {
    logger.info("Responding to status request.")
    Status()
  }
}
