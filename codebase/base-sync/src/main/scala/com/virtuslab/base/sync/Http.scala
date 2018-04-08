package com.virtuslab.base.sync

import com.virtuslab.Config

import scala.concurrent.duration.DurationInt
import scalaj.http.{HttpRequest, Http => SHttp}

object Http {
  def apply(url: String): HttpRequest = {
    SHttp(url)
      .timeout(
        connTimeoutMs = Config.httpConnectionTimeout.seconds.toMillis.toInt,
        readTimeoutMs = Config.httpResponseTimeout.seconds.toMillis.toInt
      )
  }
}
