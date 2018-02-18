package com.virtuslab.auctionhouse.primaryasync

import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

import spray.json.JsObject

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

trait TestAuctionServiceImpl extends AuctionService {
  this: IdentityHelpers =>

  private val currentAuctionId = new AtomicReference[String](UUID.randomUUID.toString)
  private val currentBidId = new AtomicReference[String](UUID.randomUUID.toString)
  private var auctions = Map.empty[String, AuctionResponse]

  def getCurrentAuctionId: String = currentAuctionId.get()

  def getCurrentBidId: String = currentBidId.get()

  def addAuction(id: String, owner: String, title: String): Unit = {
    auctions = auctions + (id -> AuctionResponse(id, owner, title, "", BigDecimal(0d), JsObject(), Nil))
  }

  def addAuctionWithBids(id: String, owner: String, title: String, bids: List[Bid]): Unit = {
    auctions = auctions + (id -> AuctionResponse(id, owner, title, "", BigDecimal(0d), JsObject(), bids))
  }

  def createAuction(command: CreateAuction): Future[String] = {
    val auctionId = currentAuctionId.get()
    val auction = AuctionResponse(
      auctionId = auctionId,
      owner = command.owner,
      title = command.title,
      description = command.description,
      minimumPrice = command.minimumPrice,
      details = command.details,
      bids = List.empty
    )

    auctions = auctions + (auctionId -> auction)
    currentAuctionId.set(UUID.randomUUID.toString)

    successful(auctionId)
  }

  def listAuctions: Future[List[AuctionInfo]] = successful {
    auctions.values.toList.map {
      case AuctionResponse(id, owner, title, _, mp, _, _) => AuctionInfo(id, owner, title, mp)
    }
  }

  def getAuction(auctionId: String): Future[AuctionResponse] = (auctions get auctionId)
    .fold(failed[AuctionResponse](AuctionNotFound(auctionId)))(successful)

  def bidInAuction(command: BidInAuction): Future[Unit] = {
    val auctionId = command.auctionId
    auctions get auctionId match {
      case Some(auction) =>
        auction.bids.sortBy(_.amount).headOption match {
          case Some(highestBid) =>
            if (highestBid.amount > command.amount)
              failed(BidTooSmall(auctionId, highestBid.amount))
            else {
              val bid = Bid(currentBidId.get, command.bidder, command.amount)
              auctions = (auctions - auctionId) + (auctionId -> auction.copy(bids = bid :: auction.bids))
              successful(())
            }
          case None =>
            val bid = Bid(currentBidId.get, command.bidder, command.amount)
            auctions = (auctions - auctionId) + (auctionId -> auction.copy(bids = bid :: Nil))
            successful(())
        }
      case None =>
        failed(AuctionNotFound(auctionId))
    }
  }

  def clearAuctionData(): Unit = {
    auctions = Map.empty
    currentAuctionId.set(UUID.randomUUID.toString)
    currentBidId.set(UUID.randomUUID.toString)
  }
}
