package com.virtuslab.auctionHouse.sync.auctions

import java.util.UUID

import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidBidException, InvalidCategoryException}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{BidRequest, CreateAuctionRequest, EntityNotFoundException, ErrorResponse}
import com.virtuslab.auctionHouse.sync.signIn.Authentication
import com.virtuslab.base.sync.BaseServlet
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.util.Try

class AuctionsServlet extends BaseServlet with Authentication {

  override protected implicit def jsonFormats: Formats = DefaultFormats

  override def servletName: String = "AuctionHouse"

  before() {
    contentType = formats("json")
  }

  lazy val auctionsService = new AuctionsService()

  get("/") {
    auth { _ =>
      val traceId = getTraceId
      val histogramTimer = requestsLatency.labels("listAuctions").startTimer()

      val result = params.get("category").map { category =>
        Try(auctionsService.listAuctions(category)).map(Ok(_))
          .recover {
            case e: InvalidCategoryException => BadRequest(e.getMessage)
          }.get
      }.getOrElse(BadRequest())

      histogramTimer.observeDuration()

      result
    }
  }

  post("/") {
    auth { account =>
      val histogramTimer = requestsLatency.labels("createAuction").startTimer()

      val auctionRequest = parsedBody.extract[CreateAuctionRequest]
      val result = Try(auctionsService.createAuction(auctionRequest, account.username)).map(id => Created(id))
        .recover {
          case e: InvalidCategoryException => BadRequest(e.getMessage)
        }.get

      histogramTimer.observeDuration()

      result
    }
  }

  post("/:id/bids") {
    auth { user =>
      val traceId = getTraceId
      val histogramTimer = requestsLatency.labels("bidInAuction").startTimer()
      val bidValue = parsedBody.extract[BidRequest].amount
      val auctionId = params("id")

      logger.info(s"[${traceId.id}] Received bid in auction request for auction '$auctionId'.")

      val attempt = Try(auctionsService.bidInAuction(UUID.fromString(auctionId), bidValue, user.username))
        .map(_ => {
          logger.info(s"[${traceId.id}] Added bid for auction '$auctionId'.")
          Created()
        })
        .recover {
          case e: EntityNotFoundException =>
            logger.warn(s"[${traceId.id}] Auction '$auctionId' was not found.")
            BadRequest(e.getMessage)
          case _: InvalidBidException =>
            logger.warn(s"[${traceId.id}] Bid was too small for auction '$auctionId'.")
            Conflict(ErrorResponse("your bid is not high enough"))
        }

      if (attempt.isFailure) {
        logger.error(s"[${traceId.id}] Error occured while adding bid for auction '$auctionId':", attempt.failed.get)
      }

      histogramTimer.observeDuration()

      attempt.get
    }
  }

  get("/:id") {
    auth { _ =>
      val traceId = getTraceId
      val histogramTimer = requestsLatency.labels("fetchAuction").startTimer()
      val auctionId = params("id")

      logger.info(s"[${traceId.id}] Got fetch request for auction '$auctionId'.")

      val attempt = Try(auctionsService.getAuction(UUID.fromString(auctionId)))
        .map(auction => {
          logger.info(s"[${traceId.id}] Fetched auction '$auctionId'.")
          Ok(auction)
        })
        .recover {
          case e: EntityNotFoundException => BadRequest(e.getMessage)
        }



      histogramTimer.observeDuration()

      attempt.get
    }
  }
}
