package com.virtuslab.billingsync

import com.virtuslab.base.sync.{Authentication, BaseServlet}
import com.virtuslab.payments.payments.{Invoice, PaymentRequest}
import com.virtuslab.{Config, TraceId}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{InternalServerError, Ok}

class BillingServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "BillingServlet"

  protected lazy val service = new BillingService
  protected lazy val s3Service = new S3Service

  Config.logAwsKeys(log)

  before() {
    contentType = formats("json")
  }

  post("/") {
    timing("billingProcessing") {
      implicit val traceId: TraceId = getTraceId
      auth { _ =>
        val billReq = parsedBody.extract[PaymentRequest]
        (for {
          _ <- service.performPayment(billReq)
          invoiceId <- s3Service.putInvoice(billReq)
        } yield Ok(Invoice(invoiceId))).recover {
          case e => InternalServerError(e.getMessage)
        }
      }.get
    }
  }
}
