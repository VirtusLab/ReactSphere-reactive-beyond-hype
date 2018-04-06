package com.virtuslab.payments

package object payments {
  case class PaymentRequest(payer: String, payee: String, amount: BigDecimal)
  case class Invoice(id: String)
}
