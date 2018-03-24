package com.virtuslab.base.sync

class StatusServlet extends BaseServlet {

  override def servletName: String = "StatusHouse"

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
