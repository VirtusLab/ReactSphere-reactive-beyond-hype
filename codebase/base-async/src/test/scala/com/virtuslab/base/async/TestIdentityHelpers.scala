package com.virtuslab.base.async

import com.virtuslab.TraceId

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.Future.successful

trait TestIdentityHelpers extends IdentityHelpers {

  private val validTokens: mutable.Map[String, String] = mutable.Map()

  def addValidToken(user: String, token: String): Unit = {
    validTokens += (token -> user)
  }

  override def validateToken(token: String)(implicit traceId: TraceId): Future[Option[String]] = {
    successful(validTokens.get(token))
  }

}
