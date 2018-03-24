package com.virtuslab.payment

import com.virtuslab.base.sync.BaseServletTest
import com.virtuslab.payments.PaymentModel.PaymentRequest
import org.json4s.jackson.Serialization.write
import org.scalatra._
import org.scalatra.test.JettyContainer

class SlowLegacyPaymentServletTest extends BaseServletTest(classOf[TestablePaymentServlet]) { scalatraTest: JettyContainer =>


  addServlet(classOf[SlowLegacyPaymentServlet], "/payment/*")

  "Sending payment" should {
    "acknowledge transaction" when {
      "correct format sent" in {
        val body = write(PaymentRequest("testUser", "payerUser", 15000))
        println(body)
        post(s"/payment", body.getBytes, jsonHeader) {
          status should equal(Ok().status)
        }
      }
    }

    "return bad request" when {
      "no body message is attached" in {
        post("/payment") {
          status should equal(InternalServerError().status)
        }
      }
    }
  }
}

class TestablePaymentServlet extends SlowLegacyPaymentServlet {
  override protected def timout = 1L
}
