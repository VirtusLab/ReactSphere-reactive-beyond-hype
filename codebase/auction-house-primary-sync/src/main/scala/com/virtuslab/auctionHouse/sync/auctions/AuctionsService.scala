package com.virtuslab.auctionHouse.sync.auctions

import java.util.UUID

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidCategoryException, UnknownOwnerException}
import com.virtuslab.auctionHouse.sync.cassandra._
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{AuctionViewResponse, Auctions, CreateAuctionRequest, EntityNotFoundException}

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

  def createAuction(auctionRequest: CreateAuctionRequest, owner: String): AuctionId = {
    assertCategory(auctionRequest.category)
    if (accountsMapper.getOption(owner).isEmpty) {
      throw new UnknownOwnerException(s"Cannot find owner: $owner")
    }
    val auction = new Auction(auctionRequest, owner)
    auctionsMapper.save(auction)
    auction.id
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
}

object AuctionsService {

  class InvalidCategoryException(msg: String) extends RuntimeException(msg)

  class UnknownOwnerException(msg: String) extends RuntimeException(msg)

}
