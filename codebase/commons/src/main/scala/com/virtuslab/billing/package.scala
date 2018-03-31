package com.virtuslab

package object billing {
  case class BillingRequest(payer: String, payee: String, amount: Int)
}
