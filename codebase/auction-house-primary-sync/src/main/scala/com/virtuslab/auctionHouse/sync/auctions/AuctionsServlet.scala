package com.virtuslab.auctionHouse.sync.auctions

import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.InvalidCategoryException
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

        Try(auctionsService.listAuctions(category)).map(a => Ok(a))
          .recover {
            case e: InvalidCategoryException => BadRequest(e.getMessage)
          }.get
      }.getOrElse(BadRequest())
    }
  }
}
