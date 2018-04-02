package com.virtuslab.billingsync

import java.util.UUID

import com.virtuslab.TraceId
import com.virtuslab.base.sync.BaseServletTest
import com.virtuslab.billingsync.BillingService.{TransactionId, UserId}
import com.virtuslab.payments.payments.PaymentRequest
import org.scalatra.{Ok, Unauthorized}

import scala.util.{Success, Try}

class BillingServletTest extends BaseServletTest(classOf[TestableBillingServlet]) {

  "Billing" should {
    "return transactionId" when {
      "user provides correct credentials" in {
        post("/", """{"payee": "u2", "payer" : "p2", "amount": 500}""", jsonHeader) {
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

  override lazy val service = new BillingService {
    override def performPayment(paymentRequest: PaymentRequest)(implicit traceId: TraceId): Try[TransactionId] = {
      Try(TransactionId.fresh)
    }
  }

  override protected lazy val s3Service = new S3Service {
    override def putInvoice(data: PaymentRequest): Try[String] = Success(UUID.randomUUID().toString)
  }
}