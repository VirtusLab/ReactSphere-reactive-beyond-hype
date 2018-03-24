package com.virtuslab.base.sync

import javax.servlet.http.HttpServlet

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.test.scalatest.ScalatraWordSpec

class BaseServletTest[T <: HttpServlet](servletClass: Class[T]) extends ScalatraWordSpec {
  protected implicit def jsonFormats: Formats = DefaultFormats

  System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog")
  System.setProperty("org.eclipse.jetty.LEVEL", "OFF")

  addServlet(servletClass, "/*")

  val jsonHeader = Seq(("Content-Type", "application/json"))
}
