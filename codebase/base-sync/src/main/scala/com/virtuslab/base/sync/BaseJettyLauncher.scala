package com.virtuslab.base.sync

import com.typesafe.scalalogging.Logger
import com.virtuslab.{Config, Logging}
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports
import org.eclipse.jetty.server.{HttpConfiguration, HttpConnectionFactory, Server, ServerConnector}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

abstract class BaseJettyLauncher(defaultPort: Int = 8080) extends Logging {
  override protected val log: Logger = Logger(getClass)

  def main(args: Array[String]) {
    val portProperty = System.getenv("http_port")
    log.info(s"http_port property read, value: ${portProperty}")

    val port = Option(portProperty).map(_.toInt).getOrElse(defaultPort)
    DefaultExports.initialize()
    val metricsServer = new HTTPServer(port + 1)

    val server = new Server

    val httpConfig = new HttpConfiguration
    httpConfig.setBlockingTimeout(Config.jettyBlockingTimout)
    httpConfig.setIdleTimeout(Config.jettyIdleTimout)
    val connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig))
    connector.setPort(port)
    server.addConnector(connector)

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
