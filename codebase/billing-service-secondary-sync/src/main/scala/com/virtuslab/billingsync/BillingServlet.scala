package com.virtuslab.billingsync

import com.virtuslab.TraceId
import com.virtuslab.base.sync.{Authentication, BaseServlet}
import com.virtuslab.payments.payments.PaymentRequest
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok}

class BillingServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "BillingServlet"

  protected lazy val service = new BillingService
  protected lazy val s3Service = new S3Service

  post("/") {
    timing("billingProcessing") {
      implicit val traceId: TraceId = getTraceId
      auth { _ =>
        val billReq = parsedBody.extract[PaymentRequest]
        (for {
          resp <- service.performPayment(billReq)
          _ <- s3Service.putInvoice(billReq)
        } yield Ok(resp.id)).recover {
          case e => InternalServerError(e.getMessage)
        }
      }.get
    }
  }
}
