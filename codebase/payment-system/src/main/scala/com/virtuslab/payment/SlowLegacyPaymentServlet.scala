package com.virtuslab.payment

import com.virtuslab.base.sync.BaseServlet
import com.virtuslab.payments.payments.PaymentRequest
import org.scalatra.Ok



class SlowLegacyPaymentServlet extends BaseServlet {

  override def servletName: String = "Payments"

  val timeoutMs = 1000L

  protected def timout = timeoutMs

  post("/") {
    timing("payment") {
      val req = parsedBody.extract[PaymentRequest]
      log.debug(s"Payment request received: payer: ${req.payer}, payee: ${req.payee}, amount (cents): ${req.amount}")
      Thread.sleep(timout)
      Ok()
    }
  }
}
