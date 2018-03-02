package com.virtuslab.auctionHouse.sync.auctions

import java.util.UUID

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.utils.UUIDs
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidBidException, InvalidCategoryException, UnknownEntityException}
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.cassandra._
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{AuctionViewResponse, Auctions, CreateAuctionRequest, EntityNotFoundException}
import com.virtuslab.auctions.Categories

import scala.collection.JavaConverters._

class AuctionsService {
  lazy val auctionsMapper = SessionManager.mapper(classOf[Auction])
  lazy val accountsMapper = SessionManager.mapper(classOf[Account])
  lazy val auctionsViewMapper = SessionManager.mapper(classOf[AuctionView])
  lazy val bidsMapper = SessionManager.mapper(classOf[Bid])
  lazy val session = SessionManager.session

  private val categoriesSet = Categories.toSet

  private def assertCategory(category: String): Unit = {
    if (!categoriesSet.contains(category)) {
      throw new InvalidCategoryException(s"Invalid category: $category")
    }
  }

  def listAuctions(category: String): Auctions = {
    assertCategory(category)
    val auctions = auctionsMapper.map(session.execute(QueryBuilder.select().all().from("auctions")
      .where(QueryBuilder.eq("category", category)).limit(10)))
      .all().asScala.toList
    Auctions(category, auctions)
  }

  private def accountExists(username: String): Boolean = accountsMapper.getOption(username).isDefined

  def createAuction(auctionRequest: CreateAuctionRequest, owner: String): UUID = {
    assertCategory(auctionRequest.category)
    if (!accountExists(owner)) {
      throw new UnknownEntityException(s"Cannot find owner: $owner")
    }
    val auction = new Auction(auctionRequest, owner)
    auctionsMapper.save(auction)
    auction.auction_id
  }

  def getAuction(id: UUID): AuctionViewResponse = {
    auctionsViewMapper.map(session.execute(QueryBuilder.select().all().from("auctions_view")
      .where(QueryBuilder.eq("auction_id", id)))).asScala.headOption
      .map { auction =>
        val bids = bidsMapper.map(session.execute(QueryBuilder.select().all().from("bids")
          .where(QueryBuilder.eq("auction_id", id)))).asScala.toSeq
        AuctionViewResponse(auction, bids)
      }.getOrElse(throw new EntityNotFoundException(s"Auction id = $id cannot be found"))
  }

  private def auctionExists(auctionId: UUID): Boolean = {
    1 == session.execute(QueryBuilder.select().countAll().from("auctions_view")
      .where(QueryBuilder.eq("auction_id", auctionId))).one().get(0, classOf[Long])
  }

  def bidInAuction(auctionId: UUID, bidValue: BigDecimal, bidder: String): Unit = {
    if(!auctionExists(auctionId)) throw new UnknownEntityException(s"Cannot find account: $bidder")
    if (!accountExists(bidder)) throw new UnknownEntityException(s"Cannot find account: $bidder")
    val isMaxBid = bidsMapper.map(session.execute(QueryBuilder.select().all().from("bids")
      .where(QueryBuilder.eq("auction_id", auctionId)))).asScala.forall(b => BigDecimal(b.amount) < bidValue)
    if (isMaxBid) {
      bidsMapper.save(new Bid(auctionId, UUIDs.timeBased(), bidder, bidValue.bigDecimal))
    } else {
      throw new InvalidBidException("Bid value is not big enough")
    }
  }
}

object AuctionsService {

  class InvalidCategoryException(msg: String) extends RuntimeException(msg)

  class InvalidBidException(msg: String) extends RuntimeException(msg)

  class UnknownEntityException(msg: String) extends RuntimeException(msg)

}
