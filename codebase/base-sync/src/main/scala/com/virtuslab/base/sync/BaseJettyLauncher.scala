package com.virtuslab.base.sync

import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

abstract class BaseJettyLauncher(defaultPort: Int = 8080) {
  def main(args: Array[String]) {
    val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(defaultPort)
    DefaultExports.initialize()
    val metricsServer = new HTTPServer(defaultPort + 1)

    val server = new Server(port)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")

    context.setEventListeners(Array(new ScalatraListener))

    server.setHandler(context)

    server.start()
    server.join()
    metricsServer.stop()
  }
}
