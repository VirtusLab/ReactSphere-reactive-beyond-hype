package com.virtuslab.billingsync

import com.virtuslab.TraceId
import com.virtuslab.base.sync.BaseServletTest
import com.virtuslab.billingsync.BillingService.TransactionId
import org.scalatra.{Ok, Unauthorized}

import scala.util.Try

class BillingServletTest extends BaseServletTest(classOf[TestableBillingServlet]) {

  "Billing" should {
    "return transactionId" when {
      "user provides correct credentials" in {
        post("/", """{"username": "u2", "password" : "p2"}""", jsonHeader) {
          status should equal(Ok().status)
          body.trim should not be empty
        }
      }
    }
  }
}

class NoAuthBillingServletTest extends BaseServletTest(classOf[BillingServlet]) {

  "Billing with real auth" should {
    "return unauthorized" when {
      "for no header passed" in {
        post("/", "", jsonHeader) {
          status should equal(Unauthorized().status)
        }
      }
    }
  }
}

class TestableBillingServlet extends BillingServlet {

  override def auth[T](fun: String => T)(implicit traceId: TraceId): T = fun("u2")

  override val service = new BillingService {
    override def performPayment(payer: BillingService.UserId, payee: BillingService.UserId)
                               (implicit traceId: TraceId): Try[TransactionId] = {
      Try(TransactionId.fresh)
    }
  }
}