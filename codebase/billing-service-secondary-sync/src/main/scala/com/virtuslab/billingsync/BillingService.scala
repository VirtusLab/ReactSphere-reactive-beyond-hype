package com.virtuslab.billingsync

import com.virtuslab.billingsync.BillingService.{TransactionId, UserId}

import scala.util.{Random, Try}

object BillingService {
  case class UserId(login: String)

  case class TransactionId(id: String)
  object TransactionId {
    val random = new Random()

    def fresh(): TransactionId = {
      TransactionId(random.alphanumeric.take(10).mkString)
    }
  }
}

class BillingService {
  def performPayment(payer: UserId, payee: UserId): Try[TransactionId] = {
    ???
  }
}
