package com.virtuslab.auctionhouse.primaryasync

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.virtuslab.Config

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration

object AuctionHouseServer extends Routes {

  lazy implicit val system: ActorSystem = ActorSystem("auctionHouseServer")
  lazy implicit val materializer: ActorMaterializer = ActorMaterializer()
  lazy implicit val executionContext: ExecutionContext = system.dispatcher

  override lazy val cassandraContactPoint: String = Config.cassandraContactPoint

  def main(args: Array[String]) {

    Http().bindAndHandle(routes, "0.0.0.0", 8080)

    println(s"Server online at http://0.0.0.0:8080/")

    Await.result(system.whenTerminated, Duration.Inf)

  }

}
