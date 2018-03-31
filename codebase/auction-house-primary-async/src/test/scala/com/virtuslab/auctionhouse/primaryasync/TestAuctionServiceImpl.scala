package com.virtuslab.auctionhouse.primaryasync

import java.util.concurrent.atomic.AtomicReference
import java.util.{Date, UUID}

import com.virtuslab.TraceId
import com.virtuslab.base.async.IdentityHelpers
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

  def addAuction(category: String, id: String, owner: String, title: String, time: Long): Unit = {
    auctions = auctions + (id -> AuctionResponse(category, id, time, owner, title, "", BigDecimal(0d), JsObject(), Nil))
  }

  def addAuctionWithBids(category: String, id: String, owner: String, title: String, bids: List[Bid], time: Long): Unit = {
    auctions = auctions + (id -> AuctionResponse(category, id, time, owner, title, "", BigDecimal(0d), JsObject(), bids))
  }

  def createAuction(command: CreateAuction)(implicit traceId: TraceId): Future[String] = {
    val auctionId = currentAuctionId.get()
    val auction = AuctionResponse(
      category = command.category,
      auctionId = auctionId,
      createdAt = (new Date).getTime,
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

  def listAuctions(category: String)(implicit traceId: TraceId): Future[List[AuctionInfo]] = successful {
    auctions.values.toList
      .filter(_.category == category)
      .map {
        case AuctionResponse(_, id, timestamp, owner, title, _, mp, _, _) => AuctionInfo(id, timestamp, owner, title, mp)
      }
  }

  def getAuction(auctionId: String)(implicit traceId: TraceId): Future[AuctionResponse] = (auctions get auctionId)
    .fold(failed[AuctionResponse](AuctionNotFound(auctionId)))(successful)

  def bidInAuction(command: BidInAuction)(implicit traceId: TraceId): Future[Unit] = {
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

  override def payForAuction(auctionId: String, bidder: String, token: String)(implicit traceId: TraceId): Future[Unit] = ???

  def clearAuctionData(): Unit = {
    auctions = Map.empty
    currentAuctionId.set(UUID.randomUUID.toString)
    currentBidId.set(UUID.randomUUID.toString)
  }
}
