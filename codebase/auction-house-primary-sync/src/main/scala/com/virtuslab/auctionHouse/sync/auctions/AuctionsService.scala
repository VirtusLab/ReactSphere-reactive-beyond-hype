package com.virtuslab.auctionHouse.sync.auctions

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.virtuslab.auctionHouse.sync.auctions.AuctionsService.{InvalidCategoryException, UnknownOwnerException}
import com.virtuslab.auctionHouse.sync.cassandra._
import com.virtuslab.auctionHouse.sync.cassandra.SessionManager.ScalaMapper
import com.virtuslab.auctionHouse.sync.commons.ServletModels.{Auctions, CreateAuctionRequest}

import scala.collection.JavaConverters._

class AuctionsService {
  lazy val auctionsMapper = SessionManager.mapper(classOf[Auction])
  lazy val accountsMapper = SessionManager.mapper(classOf[Account])
  lazy val session = SessionManager.session

  private val categoriesSet = Categories.toSet

  private def assertCategory(category: String): Unit = {
    if(!categoriesSet.contains(category)) {
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
    if(accountsMapper.getOption(owner).isEmpty) {
      throw new UnknownOwnerException(s"Cannot find owner: $owner")
    }
    val auction = new Auction(auctionRequest, owner)
    auctionsMapper.save(auction)
    auction.id
  }
}
object AuctionsService {
  class InvalidCategoryException(msg: String) extends RuntimeException(msg)
  class UnknownOwnerException(msg: String) extends RuntimeException(msg)
}
