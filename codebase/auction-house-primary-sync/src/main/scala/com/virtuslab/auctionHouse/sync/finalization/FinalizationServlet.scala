package com.virtuslab.auctionHouse.sync.finalization

import com.virtuslab.TraceId
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.NotAuctionWinnerException
import com.virtuslab.base.sync.{Authentication, BaseServlet}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.util.Try

class FinalizationServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "Finalization"

  before() {
    contentType = formats("json")
  }

  lazy val service = new AuctionsService()

  post("/:id") {
    // here this should be performed by some internal user (SYSTEM that triggers auction finalization)
    implicit val traceId: TraceId = getTraceId
    auth { username =>
      log.info(s"[${traceId.id}] Finalization triggered by '$username'.")
      timing("auctionFinalization") {
        val auctionId = params("id")
        Try(service.payForAuction(auctionId, username, getToken.get.value)).map { _ =>
          Ok()
        }.recover {
          case e: NotAuctionWinnerException =>
            log.warn(s"[${traceId.id}] Bidder '$username' is not auction '$auctionId' winner.")
            BadRequest(e.getMessage)
          case e =>
            log.error(s"[${traceId.id}] Error occurred while paying for auction '$auctionId':", e)
            InternalServerError(e.getMessage)
        }
      }.get
    }
  }
}
