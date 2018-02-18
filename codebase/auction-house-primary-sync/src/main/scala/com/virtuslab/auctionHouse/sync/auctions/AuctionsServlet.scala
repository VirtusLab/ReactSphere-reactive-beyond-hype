package com.virtuslab.auctionHouse.sync.auctions

import com.virtuslab.auctionHouse.sync.signIn.Authentication
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

class AuctionsServlet extends ScalatraServlet with JacksonJsonSupport with Authentication {
  override protected implicit def jsonFormats: Formats = DefaultFormats

  get("/") {
    auth { account =>
      Ok(Seq(s"au1_${account.username}", s"au2_${account.username}").mkString(",")) // TODO mocked
    }
  }
}
