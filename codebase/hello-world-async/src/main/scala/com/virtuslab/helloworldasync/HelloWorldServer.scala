package com.virtuslab.helloworldasync

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object HelloWorldServer extends Routes {

  def main(args: Array[String]) {
    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    Http().bindAndHandle(routes, "0.0.0.0", 8080)

    println(s"Server online at http://0.0.0.0:8080/")

    Await.result(system.whenTerminated, Duration.Inf)
  }

}
