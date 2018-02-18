package com.virtuslab.auctionhouse.primaryasync

import scala.concurrent.Future

trait IdentityHelpers {
  this: IdentityService =>

  def validateToken(token: String): Future[Option[String]]

  case class MissingTokenError(error: String = "authentication token is missing")

  case class InvalidTokenError(error: String = "authentication token is invalid")
}
