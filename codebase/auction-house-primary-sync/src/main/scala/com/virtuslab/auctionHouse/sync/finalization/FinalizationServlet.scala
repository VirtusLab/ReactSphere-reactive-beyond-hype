package com.virtuslab.auctionHouse.sync.finalization

import com.virtuslab.TraceId
import com.virtuslab.base.sync.{Authentication, BaseServlet}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

class FinalizationServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "Finalization"

  before() {
    contentType = formats("json")
  }

  lazy val service = new FinalizationService()

  post("/:id") {
    // here this should be performed by some internal user (SYSTEM that triggers auction finalization)
    implicit val traceId: TraceId = getTraceId
    auth { username =>
      log.info(s"[${traceId.id}] Finalization triggered by '$username'.")
      timing("auctionFinalization") {
        val auctionId = params("id")
        service.finalizeAuction().map { _ =>
          Ok(s"Nice, auction [${auctionId}] finalized!")
        }.recover {
          case e => InternalServerError(e.getMessage)
        }
      }.get
    }
  }
}
