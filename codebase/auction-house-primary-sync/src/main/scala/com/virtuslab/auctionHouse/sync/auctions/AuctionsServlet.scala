package com.virtuslab.auctionHouse.sync.auctions

import java.util.UUID

import com.virtuslab.auctionHouse.sync.BaseServlet
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidBidException, InvalidCategoryException}
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{BidRequest, CreateAuctionRequest, EntityNotFoundException, ErrorResponse}
import com.virtuslab.auctionHouse.sync.signIn.Authentication
import org.json4s.{DefaultFormats, Formats}
import org.scalatra._

import scala.util.Try

class AuctionsServlet extends BaseServlet with Authentication {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  lazy val auctionsService = new AuctionsService()

  get("/") {
    auth { _ =>
      params.get("category").map { category =>
        Try(auctionsService.listAuctions(category)).map(Ok(_))
          .recover {
            case e: InvalidCategoryException => BadRequest(e.getMessage)
          }.get
      }.getOrElse(BadRequest())
    }
  }

  post("/") {
    auth { account =>
      val auctionRequest = parsedBody.extract[CreateAuctionRequest]
      Try(auctionsService.createAuction(auctionRequest, account.username)).map(id => Ok(id))
        .recover {
          case e: InvalidCategoryException => BadRequest(e.getMessage)
        }.get
    }
  }

  post("/:id/bids") {
    auth { user =>
      val bidValue = parsedBody.extract[BidRequest].amount
      Try(auctionsService.bidInAuction(UUID.fromString(params("id")), bidValue, user.username))
        .map(_ => Created())
        .recover {
          case e: EntityNotFoundException => BadRequest(e.getMessage)
          case _: InvalidBidException => Conflict(ErrorResponse("your bid is not high enough"))
        }.get
    }
  }

  get("/:id") {
    auth { _ =>
      Try(auctionsService.getAuction(UUID.fromString(params("id"))))
        .map(Ok(_))
        .recover {
          case e: EntityNotFoundException => BadRequest(e.getMessage)
        }.get
    }
  }
}
