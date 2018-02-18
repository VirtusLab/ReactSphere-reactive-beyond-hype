package com.virtuslab.auctionhouse.primaryasync

import spray.json.JsObject

import scala.concurrent.Future

trait AuctionService {

  // errors
  case class AuctionNotFound(auctionId: String) extends RuntimeException(auctionId)
  case class BidTooSmall(auctionId: String, highestBidAmount: BigDecimal) extends RuntimeException(s"$auctionId:$highestBidAmount")

  // request vm
  case class CreateAuctionRequest(title: String, description: String, minimumPrice: BigDecimal, details: JsObject) {
    def addOwner(owner: String): CreateAuction = CreateAuction(owner, title, description, minimumPrice, details)
  }

  case class BidRequest(amount: BigDecimal) {
    def enrich(bidder: String, auctionId: String): BidInAuction = BidInAuction(bidder, auctionId, amount)
  }

  // commands
  case class CreateAuction(owner: String, title: String, description: String, minimumPrice: BigDecimal, details: JsObject)
  case class BidInAuction(bidder: String, auctionId: String, amount: BigDecimal)

  // response VMs
  case class CreatedAuction(auctionId: String)
  case class AuctionInfo(auctionId: String, owner: String, title: String, minimumPrice: BigDecimal)
  case class Auctions(auctions: List[AuctionInfo])
  case class AuctionResponse(auctionId: String, owner: String, title: String,
                             description: String, minimumPrice: BigDecimal,
                             details: JsObject, bids: List[Bid])
  case class Bid(bidId: String, bidder: String, amount: BigDecimal)

  def createAuction(command: CreateAuction): Future[String]

  def listAuctions: Future[List[AuctionInfo]]

  def getAuction(auctionId: String): Future[AuctionResponse]

  def bidInAuction(command: BidInAuction): Future[Unit]
}

trait AuctionServiceImpl extends AuctionService {
  this: IdentityHelpers =>

  def createAuction(command: CreateAuction): Future[String] = ???

  def listAuctions: Future[List[AuctionInfo]] = ???

  def getAuction(auctionId: String): Future[AuctionResponse] = ???

  def bidInAuction(command: BidInAuction): Future[Unit] = ???

}
