package com.virtuslab.base.async

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.{HttpExt, Http => AHttp}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}


object Http {
  def apply()(implicit system: ActorSystem, executionContext: ExecutionContext): Http = new Http(AHttp())
}

class Http(private val akkaHttp: HttpExt)(implicit executionContext: ExecutionContext) {
  def mapRequest[T](httpRequest: HttpRequest)(logic: HttpResponse => T)
                   (implicit materializer: Materializer): Future[T] = {
    akkaHttp
      .singleRequest(httpRequest)
      .map { response =>
        val result = logic(response)
        response.discardEntityBytes()
        result
      }
  }

  def flatMapRequest[T](httpRequest: HttpRequest)(logic: HttpResponse => Future[T])
                       (implicit materializer: Materializer): Future[T] = {
    mapRequest(httpRequest)(logic)
      .flatten
  }
}
