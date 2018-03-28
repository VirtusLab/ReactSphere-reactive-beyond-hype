package com.virtuslab.helloworldasync

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.Logger
import com.virtuslab.base.async.BaseServer

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object HelloWorldServer extends BaseServer with Routes {
  override protected def logger: Logger = Logger("HelloWorldServer")
}
