package com.virtuslab.auctionHouse.sync.commons

import com.virtuslab.auctionHouse.sync.cassandra
import com.virtuslab.auctionHouse.sync.cassandra.{Auction, AuctionView}
import org.json4s.JValue
import org.json4s.jackson.JsonMethods.parse

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

  case class Bid(auctionId: String, bidId: String, bidder: String, amount: BigDecimal)
  object Bid {
    def apply(b: cassandra.Bid): Bid = Bid(b.auction_id.toString, b.bid_id.toString, b.bidder, b.amount)
  }
  case class AuctionViewResponse(auctionId: String, title: String, description: String, details: JValue, bids: Seq[Bid])
  object AuctionViewResponse {
    def apply(a: AuctionView, bids: Seq[cassandra.Bid]): AuctionViewResponse = {
      new AuctionViewResponse(a.auction_id.toString, a.title, a.description, parse(a.details), bids.map(Bid(_)))
    }
  }
  case class BidRequest(amount: BigDecimal)
  class EntityNotFoundException(msg: String) extends RuntimeException(msg)
}
