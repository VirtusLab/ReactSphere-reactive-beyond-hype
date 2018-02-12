package com.virtuslab.helloworldsync

import org.scalatra._
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json._

class HelloWorldServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit lazy val jsonFormats: Formats = DefaultFormats

  private lazy val version = System.getProperty("service.version", "unknown")

  case class Status(version: String = version)
  case class HelloWorld(message: String = "Hello World!")

  before() {
    contentType = formats("json")
  }

  get("/") {
    HelloWorld()
  }

  get("/_status") {
    Status()
  }

}
