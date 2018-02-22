package com.virtuslab.auctionHouse.sync

import java.util.UUID

import com.virtuslab.auctionHouse.sync.cassandra.Auction

package object auctions {

  case class AuctionListRow(auctionId: UUID, title: String, description: String)

  object AuctionListRow {
    def apply(auction: Auction): AuctionListRow = AuctionListRow(auction.auction_id, auction.title, auction.description)
  }

}
