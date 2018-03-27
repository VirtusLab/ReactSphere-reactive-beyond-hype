package com.virtuslab.auctionHouse.perfTests

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._
class ErrorHandler {
  private val errorQueue = new ConcurrentLinkedQueue[String]()

  def raiseError(msg: String) = {
    errorQueue.add(msg)
    throw new RuntimeException(msg)
  }

  def errors = errorQueue.asScala.toSeq
}