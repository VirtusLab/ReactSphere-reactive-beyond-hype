package com.virtuslab.base.async

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.scalalogging.Logger
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

abstract class BaseServer(defaultPort: Int = 8080) {

  private val port = Option(System.getenv("http_port")).map(_.toInt).getOrElse(defaultPort)

  DefaultExports.initialize()
  private val metricsServer = new HTTPServer(port + 1)
  protected def logger: Logger

  lazy implicit val system: ActorSystem = ActorSystem("auctionHouseServer")
  lazy implicit val materializer: Materializer = ActorMaterializer()
  protected lazy implicit val executionContext: ExecutionContext = system.dispatcher

  def routes: Route

  def main(args: Array[String]) {
    Http().bindAndHandle(routes, "0.0.0.0", port)

    logger.info(s"Server online at http://0.0.0.0:$port/")

    Await.result(system.whenTerminated, Duration.Inf)
    metricsServer.stop()
  }

}
