package com.virtuslab.auctionHouse.sync

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.ScalatraServlet
import org.scalatra.json.JacksonJsonSupport
import org.slf4j.{Logger, LoggerFactory}

trait BaseServlet extends ScalatraServlet with JacksonJsonSupport {

  protected val logger: Logger = LoggerFactory.getLogger(getClass)

  override protected implicit def jsonFormats: Formats = DefaultFormats

  error {
    case e: Throwable => {
      logger.error("Unexpected error", e)
      throw e
    }
  }

}
