package com.virtuslab.auctionHouse.sync.commons

import com.virtuslab.auctionHouse.sync.cassandra.Auction
import org.json4s.JValue

object ServletModels {
  case class ErrorResponse(error: String)

  case class AuctionInfo(auctionId: String, createdAt: Long, owner: String, title: String, minimumPrice: BigDecimal)
  object AuctionInfo {
    def apply(a: Auction): AuctionInfo = new AuctionInfo(
      a.auction_id.toString, a.created_at.getTime, a.owner, a.title, a.minimum_price)
  }

  case class Auctions(category: String, auctions: Seq[AuctionInfo])
  object Auctions {
    def apply(category: String, a: Iterable[Auction]): Auctions = {
      new Auctions(category, a.map(AuctionInfo(_)).toSeq)
    }
  }
  case class CreateAuctionRequest(category: String, title: String, description: String, minimumPrice: BigDecimal,
                                  details: JValue)
}
