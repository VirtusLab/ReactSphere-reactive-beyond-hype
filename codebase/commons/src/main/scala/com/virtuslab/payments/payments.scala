package com.virtuslab.payments

package object payments {
  case class PaymentRequest(payer: String, payee: String, amount: Int)
}
