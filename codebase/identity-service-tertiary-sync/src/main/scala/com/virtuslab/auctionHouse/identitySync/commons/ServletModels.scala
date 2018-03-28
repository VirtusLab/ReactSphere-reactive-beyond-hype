package com.virtuslab.auctionHouse.identitySync.commons

object ServletModels {

  case class ErrorResponse(error: String)

  class EntityNotFoundException(msg: String) extends RuntimeException(msg)

}
