package com.virtuslab.payments

object PaymentModel {
  case class PaymentRequest(payer: String, payee: String, amount: Int)
}
