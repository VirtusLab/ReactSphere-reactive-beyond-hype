package com.virtuslab.auctionhouse.primaryasync

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import com.virtuslab.Config
import io.prometheus.client.exporter.HTTPServer
import io.prometheus.client.hotspot.DefaultExports

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object AuctionHouseServer extends Routes {

  DefaultExports.initialize()
  private val metricsServer = new HTTPServer(8081)
  protected val logger: Logger = Logger("AuctionHouse")

  lazy implicit val system: ActorSystem = ActorSystem("auctionHouseServer")
  lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  protected lazy implicit val executionContext: ExecutionContext = system.dispatcher

  override lazy val cassandraContactPoint: String = Config.cassandraContactPoint

  def main(args: Array[String]) {

    val port = Option(System.getProperty("http.port")).map(_.toInt).getOrElse(8080)

    Http().bindAndHandle(routes, "0.0.0.0", port)

    logger.info(s"Server online at http://0.0.0.0:$port/")

    Await.result(system.whenTerminated, Duration.Inf)
    metricsServer.stop()
  }

}
