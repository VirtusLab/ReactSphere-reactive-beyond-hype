package com.virtuslab.auctionHouse.sync.auctions

import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.InvalidCategoryException
import com.virtuslab.auctionHouse.sync.commons.ServletModels.CreateAuctionRequest
import com.virtuslab.auctionHouse.sync.signIn.Authentication
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{BadRequest, Ok, ScalatraServlet}

import scala.util.Try

class AuctionsServlet extends ScalatraServlet with JacksonJsonSupport with Authentication {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
  }

  lazy val auctionsService = new AuctionsService()

  get("/") {
    auth { account =>
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
      Try(auctionsService.createAuction(auctionRequest, account.username)).map(id => Ok(id.idString))
        .recover {
          case e: InvalidCategoryException => BadRequest(e.getMessage)
        }.get
    }
  }
}
