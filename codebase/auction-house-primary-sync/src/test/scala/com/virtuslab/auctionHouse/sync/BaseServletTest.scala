package com.virtuslab.auctionHouse.sync

import javax.servlet.http.HttpServlet

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.test.scalatest.ScalatraWordSpec

class BaseServletTest[T <: HttpServlet](servletClass: Class[T]) extends ScalatraWordSpec {
  protected implicit def jsonFormats: Formats = DefaultFormats

  addServlet(servletClass, "/*")

  val jsonHeader = Seq(("Content-Type", "application/json"))
}
