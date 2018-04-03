package com.virtuslab.base.sync

import com.typesafe.scalalogging.Logger
import com.virtuslab.Logging
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

abstract class BaseJettyLauncher(defaultPort: Int = 8080) extends Logging {
  override protected val log: Logger = Logger(getClass)

  def main(args: Array[String]) {
    val portProperty = System.getenv("http_port")
    log.info(s"http_port property read, value: ${portProperty}")

    val port = Option(portProperty).map(_.toInt).getOrElse(defaultPort)
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
