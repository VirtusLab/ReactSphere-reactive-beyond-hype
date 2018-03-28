package com.virtuslab.auctionHouse.sync.auctions

import java.util.UUID

import com.virtuslab.TraceId
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidBidException, InvalidCategoryException}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{BidRequest, CreateAuctionRequest, EntityNotFoundException, ErrorResponse}
import com.virtuslab.base.sync.{Authentication, BaseServlet}
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.util.{Success, Try}

class AuctionsServlet extends BaseServlet with Authentication {

  override protected implicit val jsonFormats: Formats = DefaultFormats

  override def servletName: String = "AuctionHouse"

  before() {
    contentType = formats("json")
  }

  lazy val auctionsService = new AuctionsService()

  get("/") {
    implicit val traceId: TraceId = getTraceId
    auth { username =>
      val histogramTimer = requestsLatency.labels("listAuctions").startTimer()

      val attempt = params.get("category").map { category =>
        logger.info(s"[${traceId.id}] Got list auctions request for category '$category'.")
        Try(auctionsService.listAuctions(category)).map(Ok(_))
          .recover {
            case e: InvalidCategoryException =>
              logger.info(s"[${traceId.id}] Rejecting auction creation request for user '$username' due to invalid category.")
              BadRequest(e.getMessage)
          }
      }.getOrElse {
        logger.info(s"[${traceId.id}] Rejecting list auctions request for user '$username' due to missing category.")
        Success(BadRequest())
      }

      if (attempt.isFailure) {
        logger.error(s"[${traceId.id}] Error occured while listing auctions for user '$username':", attempt.failed.get)
      }

      histogramTimer.observeDuration()

      attempt.get
    }
  }

  post("/") {
    implicit val traceId: TraceId = getTraceId
    auth { username =>
      logger.info(s"[${traceId.id}] Got create auction request for user '$username'.")
      val histogramTimer = requestsLatency.labels("createAuction").startTimer()

      val auctionRequest = parsedBody.extract[CreateAuctionRequest]
      val attempt = Try(auctionsService.createAuction(auctionRequest, username))
        .map(id => {
          logger.info(s"[${traceId.id}] Created auction '$id' for user '$username'.")
          Created(CreatedAuction(id.toString))
        })
        .recover {
          case e: InvalidCategoryException =>
            logger.info(s"[${traceId.id}] Rejecting auction creation request for user '$username' due to invalid category.")
            BadRequest(e.getMessage)
        }

      if (attempt.isFailure) {
        logger.error(s"[${traceId.id}] Error occured while creating auction for user '$username':", attempt.failed.get)
      }

      histogramTimer.observeDuration()

      attempt.get
    }
  }

  post("/:id/bids") {
    implicit val traceId: TraceId = getTraceId
    auth { username =>
      val histogramTimer = requestsLatency.labels("bidInAuction").startTimer()
      val bidValue = parsedBody.extract[BidRequest].amount
      val auctionId = params("id")

      logger.info(s"[${traceId.id}] Received bid in auction request for auction '$auctionId'.")

      val attempt = Try(auctionsService.bidInAuction(UUID.fromString(auctionId), bidValue, username))
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
    implicit val traceId: TraceId = getTraceId
    auth { _ =>
      val histogramTimer = requestsLatency.labels("fetchAuction").startTimer()
      val auctionId = params("id")

      logger.info(s"[${traceId.id}] Got fetch request for auction '$auctionId'.")

      val attempt = Try(auctionsService.getAuction(UUID.fromString(auctionId)))
        .map(auction => {
          logger.info(s"[${traceId.id}] Fetched auction '$auctionId'.")
          Ok(auction)
        })
        .recover {
          case e: EntityNotFoundException =>
            logger.warn(s"[${traceId.id}] Auction '$auctionId' was not found.")
            BadRequest(e.getMessage)
        }

      if (attempt.isFailure) {
        logger.error(s"[${traceId.id}] Error occured while adding bid for auction '$auctionId':", attempt.failed.get)
      }

      histogramTimer.observeDuration()

      attempt.get
    }
  }
}
