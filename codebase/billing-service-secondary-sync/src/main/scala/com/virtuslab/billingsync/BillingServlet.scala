package com.virtuslab.billingsync

import com.virtuslab.TraceId
import com.virtuslab.base.sync.{Authentication, BaseServlet}
import com.virtuslab.payments.payments.PaymentRequest
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok}

class BillingServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "BillingServlet"

  protected val service = new BillingService

  post("/") {
    timing("billingProcessing") {
      implicit val traceId: TraceId = getTraceId
      auth { username =>
        val billReq = parsedBody.extract[PaymentRequest]

        service.performPayment(billReq).map { r =>
          Ok(r.id)
        }.recover {
          case e => InternalServerError(e.getMessage)
        }
      }.get
      }
    }
}
