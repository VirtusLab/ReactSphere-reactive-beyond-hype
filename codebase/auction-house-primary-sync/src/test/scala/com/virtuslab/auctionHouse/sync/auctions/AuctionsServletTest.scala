package com.virtuslab.auctionHouse.sync.auctions

import com.virtuslab.auctionHouse.sync.BaseServletTest

class AuctionsServletTest extends BaseServletTest(classOf[AuctionsServlet]) {

  "Listing auctions" should {
    "return unauthorized" when {
      "user is not logged" in {
        get("/") {
          status should equal(401)
        }
      }
    }
  }
}

